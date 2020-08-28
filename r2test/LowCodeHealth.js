

import type {
  Instance,
  TextInstance,
  SuspenseInstance,
  Container,
  ChildSet,
  UpdatePayload,
} from './ReactFiberHostConfig';
import type {Fiber} from './ReactInternalTypes';
import type {FiberRoot} from './ReactInternalTypes';
import type {Lanes} from './ReactFiberLane';
import type {SuspenseState} from './ReactFiberSuspenseComponent.old';
import type {UpdateQueue} from './ReactUpdateQueue.old';
import type {FunctionComponentUpdateQueue} from './ReactFiberHooks.old';
import type {Wakeable} from 'shared/ReactTypes';
import type {ReactPriorityLevel} from './ReactInternalTypes';
import type {OffscreenState} from './ReactFiberOffscreenComponent';

import {unstable_wrap as Schedule_tracing_wrap} from 'scheduler/tracing';
import {
  enableSchedulerTracing,
  enableProfilerTimer,
  enableProfilerCommitHooks,
  enableSuspenseServerRenderer,
  enableFundamentalAPI,
  enableSuspenseCallback,
  enableScopeAPI,
} from 'shared/ReactFeatureFlags';
import {
  FunctionComponent,
  ForwardRef,
  ClassComponent,
  HostRoot,
  HostComponent,
  HostText,
  HostPortal,
  Profiler,
  SuspenseComponent,
  DehydratedFragment,
  IncompleteClassComponent,
  MemoComponent,
  SimpleMemoComponent,
  SuspenseListComponent,
  FundamentalComponent,
  ScopeComponent,
  Block,
  OffscreenComponent,
  LegacyHiddenComponent,
} from './ReactWorkTags';
import {
  invokeGuardedCallback,
  hasCaughtError,
  clearCaughtError,
} from 'shared/ReactErrorUtils';
import {
  NoEffect,
  ContentReset,
  Placement,
  Snapshot,
  Update,
} from './ReactSideEffectTags';
import getComponentName from 'shared/getComponentName';
import invariant from 'shared/invariant';

import {onCommitUnmount} from './ReactFiberDevToolsHook.old';
import {resolveDefaultProps} from './ReactFiberLazyComponent.old';
import {
  getCommitTime,
  recordLayoutEffectDuration,
  startLayoutEffectTimer,
} from './ReactProfilerTimer.old';
import {ProfileMode} from './ReactTypeOfMode';
import {commitUpdateQueue} from './ReactUpdateQueue.old';
