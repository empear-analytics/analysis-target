int main() { }

int f(){return 0;}

class ZeroCopyMMapMemStoreReal : public ZeroCopyMemStore {
 public:
  ZeroCopyMMapMemStoreReal(size_t entries, size_t size) {
    // we just need a socket so the kernel
    // will set the vma->vm_ops = &tcp_vm_ops
    int fd = ::socket(AF_INET, SOCK_STREAM, 0);
    if (fd >= 0) {
      void* addr =
          ::mmap(nullptr, entries * size, PROT_READ, MAP_SHARED, fd, 0);
      ::close(fd);
      if (addr != MAP_FAILED) {
        addr_ = addr;
        numEntries_ = entries;
        entrySize_ = size;
        entries_.resize(numEntries_);
        for (size_t i = 0; i < numEntries_; i++) {
          entries_[i].data =
              reinterpret_cast<uint8_t*>(addr_) + (i * entrySize_);
          entries_[i].capacity = entrySize_;
          entries_[i].store = this;
          avail_.push_back(&entries_[i]);
        }
      }
    }
  }
  ~ZeroCopyMMapMemStoreReal() override {
    CHECK_EQ(avail_.size(), numEntries_);
    if (addr_) {
      ::munmap(addr_, numEntries_ * entrySize_);
    }
  }

  ZeroCopyMemStore::EntryPtr get() override {
    std::unique_lock<std::mutex> lk(availMutex_);
    if (!avail_.empty()) {
      auto* entry = avail_.front();
      avail_.pop_front();
      DCHECK(entry->len == 0);
      DCHECK(entry->capacity == entrySize_);

      ZeroCopyMemStore::EntryPtr ret(entry);

      return ret;
    }

    return nullptr;
  }

  void put(ZeroCopyMemStore::Entry* entry) override {
    if (entry) {
      DCHECK(entry->store == this);
      if (entry->len) {
        auto ret = ::madvise(entry->data, entry->len, MADV_DONTNEED);
        entry->len = 0;
        DCHECK(!ret);
      }

      std::unique_lock<std::mutex> lk(availMutex_);
      avail_.push_back(entry);
    }
  }

 private:
  std::vector<ZeroCopyMemStore::Entry> entries_;
  std::mutex availMutex_;
  std::deque<ZeroCopyMemStore::Entry*> avail_;
  void* addr_{nullptr};
  size_t numEntries_{0};
  size_t entrySize_{0};
};

using ZeroCopyMMapMemStore = ZeroCopyMMapMemStoreReal;
#else
using ZeroCopyMMapMemStore = ZeroCopyMMapMemStoreFallback;
#endif
} // namespace

#if FOLLY_HAVE_VLA
#define FOLLY_HAVE_VLA_01 1
#else
#define FOLLY_HAVE_VLA_01 0
#endif

using std::string;
using std::unique_ptr;

namespace fsp = folly::portability::sockets;

namespace folly {

static constexpr bool msgErrQueueSupported =
#ifdef FOLLY_HAVE_MSG_ERRQUEUE
    true;
#else
    false;
#endif // FOLLY_HAVE_MSG_ERRQUEUE

std::unique_ptr<ZeroCopyMemStore> AsyncSocket::createDefaultZeroCopyMemStore(
    size_t entries, size_t size) {
  return std::make_unique<ZeroCopyMMapMemStore>(entries, size);
}

static AsyncSocketException const& getSocketClosedLocallyEx() {
  static auto& ex = *new AsyncSocketException(
      AsyncSocketException::END_OF_FILE, "socket closed locally");
  return ex;
}

static AsyncSocketException const& getSocketShutdownForWritesEx() {
  static auto& ex = *new AsyncSocketException(
      AsyncSocketException::END_OF_FILE, "socket shutdown for writes");
  return ex;
}

namespace {
#if FOLLY_HAVE_SO_TIMESTAMPING
const sock_extended_err* FOLLY_NULLABLE
cmsgToSockExtendedErr(const cmsghdr& cmsg) {
  if ((cmsg.cmsg_level == SOL_IP && cmsg.cmsg_type == IP_RECVERR) ||
      (cmsg.cmsg_level == SOL_IPV6 && cmsg.cmsg_type == IPV6_RECVERR) ||
      (cmsg.cmsg_level == SOL_PACKET &&
       cmsg.cmsg_type == PACKET_TX_TIMESTAMP)) {
    return reinterpret_cast<const sock_extended_err*>(CMSG_DATA(&cmsg));
  }
  (void)cmsg;
  return nullptr;
}

const sock_extended_err* FOLLY_NULLABLE
cmsgToSockExtendedErrTimestamping(const cmsghdr& cmsg) {
  const auto serr = cmsgToSockExtendedErr(cmsg);
  if (serr && serr->ee_errno == ENOMSG &&
      serr->ee_origin == SO_EE_ORIGIN_TIMESTAMPING) {
    return serr;
  }
  (void)cmsg;
  return nullptr;
}

const scm_timestamping* FOLLY_NULLABLE
cmsgToScmTimestamping(const cmsghdr& cmsg) {
  if (cmsg.cmsg_level == SOL_SOCKET && cmsg.cmsg_type == SCM_TIMESTAMPING) {
    return reinterpret_cast<const struct scm_timestamping*>(CMSG_DATA(&cmsg));
  }
  (void)cmsg;
  return nullptr;
}

#endif // FOLLY_HAVE_SO_TIMESTAMPING
} // namespace

// TODO: It might help performance to provide a version of BytesWriteRequest
// that users could derive from, so we can avoid the extra allocation for each
// call to write()/writev().
//
// We would need the version for external users where they provide the iovec
// storage space, and only our internal version would allocate it at the end of
// the WriteRequest.

/* The default WriteRequest implementation, used for write(), writev() and
 * writeChain()
 *
 * A new BytesWriteRequest operation is allocated on the heap for all write
 * operations that cannot be completed immediately.
 */
class AsyncSocket::BytesWriteRequest : public AsyncSocket::WriteRequest {
 public:
  static BytesWriteRequest* newRequest(
      AsyncSocket* socket,
      WriteCallback* callback,
      const iovec* ops,
      uint32_t opCount,
      uint32_t partialWritten,
      uint32_t bytesWritten,
      unique_ptr<IOBuf>&& ioBuf,
      WriteFlags flags) {
    assert(opCount > 0);
    // Since we put a variable size iovec array at the end
    // of each BytesWriteRequest, we have to manually allocate the memory.
    void* buf =
        malloc(sizeof(BytesWriteRequest) + (opCount * sizeof(struct iovec)));
    if (buf == nullptr) {
      throw std::bad_alloc();
    }

    return new (buf) BytesWriteRequest(
        socket,
        callback,
        ops,
        opCount,
        partialWritten,
        bytesWritten,
        std::move(ioBuf),
        flags);
  }

  void destroy() override {
    socket_->releaseIOBuf(std::move(ioBuf_), releaseIOBufCallback_);
    this->~BytesWriteRequest();
    free(this);
  }

  WriteResult performWrite() override {
    WriteFlags writeFlags = flags_;
    if (getNext() != nullptr) {
      writeFlags |= WriteFlags::CORK;
    }

    socket_->adjustZeroCopyFlags(writeFlags);

    auto writeResult = socket_->performWrite(
        getOps(), getOpCount(), writeFlags, &opsWritten_, &partialBytes_);
    bytesWritten_ = writeResult.writeReturn > 0 ? writeResult.writeReturn : 0;
    if (bytesWritten_) {
      if (socket_->isZeroCopyRequest(writeFlags)) {
        if (isComplete()) {
          socket_->addZeroCopyBuf(std::move(ioBuf_), releaseIOBufCallback_);
        } else {
          socket_->addZeroCopyBuf(ioBuf_.get());
        }
      } else {
        // this happens if at least one of the prev requests were sent
        // with zero copy but not the last one
        if (isComplete() && zeroCopyRequest_ &&
            socket_->containsZeroCopyBuf(ioBuf_.get())) {
          socket_->setZeroCopyBuf(std::move(ioBuf_), releaseIOBufCallback_);
        }
      }
    }
    return writeResult;
  }

  bool isComplete() override { return opsWritten_ == getOpCount(); }

  void consume() override {
    // Advance opIndex_ forward by opsWritten_
    opIndex_ += opsWritten_;
    assert(opIndex_ < opCount_);

    bool zeroCopyReq = socket_->isZeroCopyRequest(flags_);
    if (zeroCopyReq) {
      zeroCopyRequest_ = true;
    }

    if (!zeroCopyRequest_) {
      // If we've finished writing any IOBufs, release them
      // but only if we did not send any of them via zerocopy
      if (ioBuf_) {
        for (uint32_t i = opsWritten_; i != 0; --i) {
          assert(ioBuf_);
          auto next = ioBuf_->pop();
          socket_->releaseIOBuf(std::move(ioBuf_), releaseIOBufCallback_);
          ioBuf_ = std::move(next);
        }
      }
    }

    // Move partialBytes_ forward into the current iovec buffer
    struct iovec* currentOp = writeOps_ + opIndex_;
    assert((partialBytes_ < currentOp->iov_len) || (currentOp->iov_len == 0));
    currentOp->iov_base =
        reinterpret_cast<uint8_t*>(currentOp->iov_base) + partialBytes_;
    currentOp->iov_len -= partialBytes_;

    // Increment the totalBytesWritten_ count by bytesWritten_;
    assert(bytesWritten_ >= 0);
    totalBytesWritten_ += uint32_t(bytesWritten_);
  }

 private:
  BytesWriteRequest(
      AsyncSocket* socket,
      WriteCallback* callback,
      const struct iovec* ops,
      uint32_t opCount,
      uint32_t partialBytes,
      uint32_t bytesWritten,
      unique_ptr<IOBuf>&& ioBuf,
      WriteFlags flags)
      : AsyncSocket::WriteRequest(socket, callback),
        opCount_(opCount),
        opIndex_(0),
        flags_(flags),
        ioBuf_(std::move(ioBuf)),
        opsWritten_(0),
        partialBytes_(partialBytes),
        bytesWritten_(bytesWritten) {
    memcpy(writeOps_, ops, sizeof(*ops) * opCount_);
    zeroCopyRequest_ = socket_->isZeroCopyRequest(flags_);
  }

  // private destructor, to ensure callers use destroy()
  ~BytesWriteRequest() override = default;

  const struct iovec* getOps() const {
    assert(opCount_ > opIndex_);
    return writeOps_ + opIndex_;
  }

  uint32_t getOpCount() const {
    assert(opCount_ > opIndex_);
    return opCount_ - opIndex_;
  }

  uint32_t opCount_; ///< number of entries in writeOps_
  uint32_t opIndex_; ///< current index into writeOps_
  WriteFlags flags_; ///< set for WriteFlags
  bool zeroCopyRequest_{
      false}; ///< if we sent any part of the ioBuf_ with zerocopy
  unique_ptr<IOBuf> ioBuf_; ///< underlying IOBuf, or nullptr if N/A

  // for consume(), how much we wrote on the last write
  uint32_t opsWritten_; ///< complete ops written
  uint32_t partialBytes_; ///< partial bytes of incomplete op written
  ssize_t bytesWritten_; ///< bytes written altogether

  struct iovec writeOps_[]; ///< write operation(s) list
};

int AsyncSocket::SendMsgParamsCallback::getDefaultFlags(
    folly::WriteFlags flags, bool zeroCopyEnabled) noexcept {
  int msg_flags = MSG_DONTWAIT;

#ifdef MSG_NOSIGNAL // Linux-only
  msg_flags |= MSG_NOSIGNAL;
#ifdef MSG_MORE
  if (isSet(flags, WriteFlags::CORK)) {
    // MSG_MORE tells the kernel we have more data to send, so wait for us to
    // give it the rest of the data rather than immediately sending a partial
    // frame, even when TCP_NODELAY is enabled.
    msg_flags |= MSG_MORE;
  }
#endif // MSG_MORE
#endif // MSG_NOSIGNAL
  if (isSet(flags, WriteFlags::EOR)) {
    // marks that this is the last byte of a record (response)
    msg_flags |= MSG_EOR;
  }

  if (zeroCopyEnabled && isSet(flags, WriteFlags::WRITE_MSG_ZEROCOPY)) {
    msg_flags |= MSG_ZEROCOPY;
  }

  return msg_flags;
}


