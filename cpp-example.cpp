WriteResult performWrite() {
  WriteFlags writeFlags = flags_;
  if (getNext() != nullptr) {
    writeFlags |= WriteFlags::CORK;
  }

  if (x && y && c && d) {
    while (x) {
      if (x > 2) {

        if (one_more_go) {
          c = 2;
        }

        return;
      }
    }
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

WriteResult performWrite2() {
  WriteFlags writeFlags = flags_;
  if (getNext() != nullptr) {
    writeFlags |= WriteFlags::CORK;
  }

  if (x && y && c && d) {
    while (x) {
      if (x > 2) {
        return;
      }
    }
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

WriteResult performWrite3() {
  WriteFlags writeFlags = flags_;
  if (getNext() != nullptr) {
    writeFlags |= WriteFlags::CORK;
  }

  if (x && y && c && d) {
    while (x) {
      if (x > 2) {
        return;
      }
    }
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

// Let's make some Primitive Obsession here.
//

int a(int a, int b, int c) {
  return a + b;
}


int b(int a, int b, int c) {
  return a + b;
}


int c(int a, int b, int c) {
  return a + b;
}


int d(int a, int b, int c) {
  return a + b;
}


int e(int a, int b, int c) {
  return a + b;
}


int f(int a, int b, int c) {
  return a + b;
}


int g(int a, int b, int c) {
  return a + b;
}


int h(int a, int b, int c) {
  return a + b;
}

