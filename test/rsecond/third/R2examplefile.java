

        ReplyHeader hdr = new ReplyHeader(request.cxid, lastZxid, err.intValue());

        updateStats(request, lastOp, lastZxid);

        try {
            if (path == null || rsp == null) {
                responseSize = cnxn.sendResponse(hdr, rsp, "response");
            } else {
                int opCode = request.type;
                Stat stat = null;
                // Serialized read and get children responses could be cached by the connection
                // object. Cache entries are identified by their path and last modified zxid,
                // so these values are passed along with the response.
                switch (opCode) {
                    case OpCode.getData : {
                        GetDataResponse getDataResponse = (GetDataResponse) rsp;
                        stat = getDataResponse.getStat();
                        responseSize = cnxn.sendResponse(hdr, rsp, "response", path, stat, opCode);
                        break;
                    }
                    case OpCode.getChildren2 : {
                        GetChildren2Response getChildren2Response = (GetChildren2Response) rsp;
                        stat = getChildren2Response.getStat();
                        responseSize = cnxn.sendResponse(hdr, rsp, "response", path, stat, opCode);
                        break;
                    }
                    default:
                        responseSize = cnxn.sendResponse(hdr, rsp, "response");
                }
            }

            if (request.type == OpCode.closeSession) {
                cnxn.sendCloseSession();
            }
        } catch (IOException e) {
            LOG.error("FIXMSG", e);
        } finally {
            ServerMetrics.getMetrics().RESPONSE_BYTES.add(responseSize);
        }
    }

    private Record handleGetChildrenRequest(Record request, ServerCnxn cnxn, List<Id> authInfo) throws KeeperException, IOException {
        GetChildrenRequest getChildrenRequest = (GetChildrenRequest) request;
        String path = getChildrenRequest.getPath();
        DataNode n = zks.getZKDatabase().getNode(path);
        if (n == null) {
            throw new KeeperException.NoNodeException();
        }
        zks.checkACL(cnxn, zks.getZKDatabase().aclForNode(n), ZooDefs.Perms.READ, authInfo, path, null);
        List<String> children = zks.getZKDatabase()
                                   .getChildren(path, null, getChildrenRequest.getWatch() ? cnxn : null);
        return new GetChildrenResponse(children);
    }

    private Record handleGetDataRequest(Record request, ServerCnxn cnxn, List<Id> authInfo) throws KeeperException, IOException {
        GetDataRequest getDataRequest = (GetDataRequest) request;
        String path = getDataRequest.getPath();
        DataNode n = zks.getZKDatabase().getNode(path);
        if (n == null) {
            throw new KeeperException.NoNodeException();
        }
        zks.checkACL(cnxn, zks.getZKDatabase().aclForNode(n), ZooDefs.Perms.READ, authInfo, path, null);
        Stat stat = new Stat();
        byte[] b = zks.getZKDatabase().getData(path, stat, getDataRequest.getWatch() ? cnxn : null);
        return new GetDataResponse(b, stat);
    }

    private boolean closeSession(ServerCnxnFactory serverCnxnFactory, long sessionId) {
        if (serverCnxnFactory == null) {
            return false;
        }
        return serverCnxnFactory.closeSession(sessionId, ServerCnxn.DisconnectReason.CLIENT_CLOSED_SESSION);
    }

    private boolean connClosedByClient(Request request) {
        return request.cnxn == null;
    }

    public void shutdown() {
        // we are the final link in the chain
        LOG.info("shutdown of request processor complete");
    }

    private void updateStats(Request request, String lastOp, long lastZxid) {
        if (request.cnxn == null) {
            return;
        }
        long currentTime = Time.currentElapsedTime();
        zks.serverStats().updateLatency(request, currentTime);
        request.cnxn.updateStatsForResponse(request.cxid, lastZxid, lastOp, request.createTime, currentTime);
    }
   public void parseProperties(Properties zkProp) throws IOException, ConfigException {
        int clientPort = 0;
        int secureClientPort = 0;
        int observerMasterPort = 0;
        String clientPortAddress = null;
        String secureClientPortAddress = null;
        VerifyingFileFactory vff = new VerifyingFileFactory.Builder(LOG).warnForRelativePath().build();
        for (Entry<Object, Object> entry : zkProp.entrySet()) {
            String key = entry.getKey().toString().trim();
            String value = entry.getValue().toString().trim();
            if (key.equals("dataDir")) {
                dataDir = vff.create(value);
            } else if (key.equals("dataLogDir")) {
                dataLogDir = vff.create(value);
            } else if (key.equals("clientPort")) {
                clientPort = Integer.parseInt(value);
            } else if (key.equals("localSessionsEnabled")) {
                localSessionsEnabled = parseBoolean(key, value);
            } else if (key.equals("localSessionsUpgradingEnabled")) {
                localSessionsUpgradingEnabled = parseBoolean(key, value);
            } else if (key.equals("clientPortAddress")) {
                clientPortAddress = value.trim();
            } else if (key.equals("secureClientPort")) {
                secureClientPort = Integer.parseInt(value);
            } else if (key.equals("secureClientPortAddress")) {
                secureClientPortAddress = value.trim();
            } else if (key.equals("observerMasterPort")) {
                observerMasterPort = Integer.parseInt(value);
            } else if (key.equals("clientPortListenBacklog")) {
                clientPortListenBacklog = Integer.parseInt(value);
            } else if (key.equals("tickTime")) {
                tickTime = Integer.parseInt(value);
            } else if (key.equals("maxClientCnxns")) {
                maxClientCnxns = Integer.parseInt(value);
            } else if (key.equals("minSessionTimeout")) {
                minSessionTimeout = Integer.parseInt(value);
            } else if (key.equals("maxSessionTimeout")) {
                maxSessionTimeout = Integer.parseInt(value);
            } else if (key.equals("initLimit")) {
                initLimit = Integer.parseInt(value);
            } else if (key.equals("syncLimit")) {
                syncLimit = Integer.parseInt(value);
            } else if (key.equals("connectToLearnerMasterLimit")) {
                connectToLearnerMasterLimit = Integer.parseInt(value);
            } else if (key.equals("electionAlg")) {
                electionAlg = Integer.parseInt(value);
                if (electionAlg != 3) {
                    throw new ConfigException("Invalid electionAlg value. Only 3 is supported.");
                }
            } else if (key.equals("quorumListenOnAllIPs")) {
                quorumListenOnAllIPs = parseBoolean(key, value);
            } else if (key.equals("peerType")) {
                if (value.toLowerCase().equals("observer")) {
                    peerType = LearnerType.OBSERVER;
                } else if (value.toLowerCase().equals("participant")) {
                    peerType = LearnerType.PARTICIPANT;
                } else {
                    throw new ConfigException("Unrecognised peertype: " + value);
                }
            } else if (key.equals("syncEnabled")) {
                syncEnabled = parseBoolean(key, value);
            } else if (key.equals("dynamicConfigFile")) {
                dynamicConfigFileStr = value;
            } else if (key.equals("autopurge.snapRetainCount")) {
                snapRetainCount = Integer.parseInt(value);
            } else if (key.equals("autopurge.purgeInterval")) {
                purgeInterval = Integer.parseInt(value);
            } else if (key.equals("standaloneEnabled")) {
                setStandaloneEnabled(parseBoolean(key, value));
            } else if (key.equals("reconfigEnabled")) {
                setReconfigEnabled(parseBoolean(key, value));
            } else if (key.equals("sslQuorum")) {
                sslQuorum = parseBoolean(key, value);
            } else if (key.equals("portUnification")) {
                shouldUsePortUnification = parseBoolean(key, value);
            } else if (key.equals("sslQuorumReloadCertFiles")) {
                sslQuorumReloadCertFiles = parseBoolean(key, value);
            } else if ((key.startsWith("server.") || key.startsWith("group") || key.startsWith("weight"))
                       && zkProp.containsKey("dynamicConfigFile")) {
                throw new ConfigException("parameter: " + key + " must be in a separate dynamic config file");
            } else if (key.equals(QuorumAuth.QUORUM_SASL_AUTH_ENABLED)) {
                quorumEnableSasl = parseBoolean(key, value);
            } else if (key.equals(QuorumAuth.QUORUM_SERVER_SASL_AUTH_REQUIRED)) {
                quorumServerRequireSasl = parseBoolean(key, value);
            } else if (key.equals(QuorumAuth.QUORUM_LEARNER_SASL_AUTH_REQUIRED)) {
                quorumLearnerRequireSasl = parseBoolean(key, value);
            } else if (key.equals(QuorumAuth.QUORUM_LEARNER_SASL_LOGIN_CONTEXT)) {
                quorumLearnerLoginContext = value;
            } else if (key.equals(QuorumAuth.QUORUM_SERVER_SASL_LOGIN_CONTEXT)) {
                quorumServerLoginContext = value;
            } else if (key.equals(QuorumAuth.QUORUM_KERBEROS_SERVICE_PRINCIPAL)) {
                quorumServicePrincipal = value;
            } else if (key.equals("quorum.cnxn.threads.size")) {
                quorumCnxnThreadsSize = Integer.parseInt(value);
            } else if (key.equals(JvmPauseMonitor.INFO_THRESHOLD_KEY)) {
                jvmPauseInfoThresholdMs = Long.parseLong(value);
            } else if (key.equals(JvmPauseMonitor.WARN_THRESHOLD_KEY)) {
                jvmPauseWarnThresholdMs = Long.parseLong(value);
            } else if (key.equals(JvmPauseMonitor.SLEEP_TIME_MS_KEY)) {
                jvmPauseSleepTimeMs = Long.parseLong(value);
            } else if (key.equals(JvmPauseMonitor.JVM_PAUSE_MONITOR_FEATURE_SWITCH_KEY)) {
                jvmPauseMonitorToRun = parseBoolean(key, value);
            } else if (key.equals("metricsProvider.className")) {
                metricsProviderClassName = value;
            } else if (key.startsWith("metricsProvider.")) {
                String keyForMetricsProvider = key.substring(16);
                metricsProviderConfiguration.put(keyForMetricsProvider, value);
            } else if (key.equals("multiAddress.enabled")) {
                multiAddressEnabled = parseBoolean(key, value);
            } else if (key.equals("multiAddress.reachabilityCheckTimeoutMs")) {
                multiAddressReachabilityCheckTimeoutMs = Integer.parseInt(value);
            } else if (key.equals("multiAddress.reachabilityCheckEnabled")) {
                multiAddressReachabilityCheckEnabled = parseBoolean(key, value);
            } else {
                System.setProperty("zookeeper." + key, value);
            }
        }

        if (!quorumEnableSasl && quorumServerRequireSasl) {
            throw new IllegalArgumentException(QuorumAuth.QUORUM_SASL_AUTH_ENABLED
                                               + " is disabled, so cannot enable "
                                               + QuorumAuth.QUORUM_SERVER_SASL_AUTH_REQUIRED);
        }
        if (!quorumEnableSasl && quorumLearnerRequireSasl) {
            throw new IllegalArgumentException(QuorumAuth.QUORUM_SASL_AUTH_ENABLED
                                               + " is disabled, so cannot enable "
                                               + QuorumAuth.QUORUM_LEARNER_SASL_AUTH_REQUIRED);
        }
        // If quorumpeer learner is not auth enabled then self won't be able to
        // join quorum. So this condition is ensuring that the quorumpeer learner
        // is also auth enabled while enabling quorum server require sasl.
        if (!quorumLearnerRequireSasl && quorumServerRequireSasl) {
            throw new IllegalArgumentException(QuorumAuth.QUORUM_LEARNER_SASL_AUTH_REQUIRED
                                               + " is disabled, so cannot enable "
                                               + QuorumAuth.QUORUM_SERVER_SASL_AUTH_REQUIRED);
        }

        // Reset to MIN_SNAP_RETAIN_COUNT if invalid (less than 3)
        // PurgeTxnLog.purge(File, File, int) will not allow to purge less
        // than 3.
        if (snapRetainCount < MIN_SNAP_RETAIN_COUNT) {
            LOG.warn("Invalid autopurge.snapRetainCount: "
                     + snapRetainCount
                     + ". Defaulting to "
                     + MIN_SNAP_RETAIN_COUNT);
            snapRetainCount = MIN_SNAP_RETAIN_COUNT;
        }

        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir is not set");
        }
        if (dataLogDir == null) {
            dataLogDir = dataDir;
        }

        if (clientPort == 0) {
            LOG.info("clientPort is not set");
            if (clientPortAddress != null) {
                throw new IllegalArgumentException("clientPortAddress is set but clientPort is not set");
            }
        } else if (clientPortAddress != null) {
            this.clientPortAddress = new InetSocketAddress(InetAddress.getByName(clientPortAddress), clientPort);
            LOG.info("clientPortAddress is {}", formatInetAddr(this.clientPortAddress));
        } else {
            this.clientPortAddress = new InetSocketAddress(clientPort);
            LOG.info("clientPortAddress is {}", formatInetAddr(this.clientPortAddress));
        }
}
    clock_serv_t cclock;
  mach_timespec_t mts;
  ret = host_get_clock_service(mach_host_self(), SYSTEM_CLOCK, &cclock);
  if (!ret) {
    ret += clock_get_time(cclock, &mts);
    ret += mach_port_deallocate(mach_task_self(), cclock);
    if (!ret) {
      tv->tv_sec = mts.tv_sec;
      tv->tv_usec = mts.tv_nsec / 1000;
    }
  }
  if (ret) {
    // Default to gettimeofday in case of failure.
    ret = gettimeofday(tv, NULL);
  }
#elif defined CLOCK_MONOTONIC_RAW
  // On Linux, CLOCK_MONOTONIC is affected by ntp slew but CLOCK_MONOTONIC_RAW
  // is not.  We want the non-slewed (constant rate) CLOCK_MONOTONIC_RAW if it
  // is available.
  struct timespec ts = { 0 };
  ret = clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
  tv->tv_sec = ts.tv_sec;
  tv->tv_usec = ts.tv_nsec / 1000;
#elif _POSIX_MONOTONIC_CLOCK
  struct timespec ts = { 0 };
  ret = clock_gettime(CLOCK_MONOTONIC, &ts);
  tv->tv_sec = ts.tv_sec;
  tv->tv_usec = ts.tv_nsec / 1000;
#elif _WIN32
  LARGE_INTEGER counts, countsPerSecond, countsPerMicrosecond;
  if (QueryPerformanceFrequency(&countsPerSecond) &&
      QueryPerformanceCounter(&counts)) {
    countsPerMicrosecond.QuadPart = countsPerSecond.QuadPart / 1000000;
    tv->tv_sec = (long)(counts.QuadPart / countsPerSecond.QuadPart);
    tv->tv_usec = (long)((counts.QuadPart % countsPerSecond.QuadPart) /
        countsPerMicrosecond.QuadPart);
    ret = 0;
  } else {
    ret = gettimeofday(tv, NULL);
  }
#else
  ret = gettimeofday(tv, NULL);
#endif
  if (ret) {
    abort();
  }
}

const void *zoo_get_context(zhandle_t *zh)
{
    return zh->context;
}

void zoo_set_context(zhandle_t *zh, void *context)
{
    if (zh != NULL) {
        zh->context = context;
    }
}

int zoo_recv_timeout(zhandle_t *zh)
{
    return zh->recv_timeout;
}

/** these functions are thread unsafe, so make sure that
    zoo_lock_auth is called before you access them **/
static auth_info* get_last_auth(auth_list_head_t *auth_list) {
    auth_info *element;
    element = auth_list->auth;
    if (element == NULL) {
        return NULL;
    }
    while (element->next != NULL) {
        element = element->next;
    }
    return element;
}

