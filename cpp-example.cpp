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

static BytesWriteRequest* newRequest2(
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

static BytesWriteRequest* newRequest3(
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

static BytesWriteRequest* newRequest4(
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

