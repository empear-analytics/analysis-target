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
import {
  getPublicInstance,
  supportsMutation,
  supportsPersistence,
  supportsHydration,
  commitMount,
  commitUpdate,
  resetTextContent,
  commitTextUpdate,
  appendChild,
  appendChildToContainer,
  insertBefore,
  insertInContainerBefore,
  removeChild,
  removeChildFromContainer,
  clearSuspenseBoundary,
  clearSuspenseBoundaryFromContainer,
  replaceContainerChildren,
  createContainerChildSet,
  hideInstance,
  hideTextInstance,
  unhideInstance,
  unhideTextInstance,
  unmountFundamentalComponent,
  updateFundamentalComponent,
  commitHydratedContainer,
  commitHydratedSuspenseInstance,
  clearContainer,
  prepareScopeUpdate,
} from './ReactFiberHostConfig';
import {
  captureCommitPhaseError,
  resolveRetryWakeable,
  markCommitTimeOfFallback,
  enqueuePendingPassiveHookEffectMount,
  enqueuePendingPassiveHookEffectUnmount,
  enqueuePendingPassiveProfilerEffect,
} from './ReactFiberWorkLoop.old';
import {
  NoEffect as NoHookEffect,
  HasEffect as HookHasEffect,
  Layout as HookLayout,
  Passive as HookPassive,
} from './ReactHookEffectTags';
import {didWarnAboutReassigningProps} from './ReactFiberBeginWork.old';

let didWarnAboutUndefinedSnapshotBeforeUpdate: Set<mixed> | null = null;
if (__DEV__) {
  didWarnAboutUndefinedSnapshotBeforeUpdate = new Set();
}

const PossiblyWeakSet = typeof WeakSet === 'function' ? WeakSet : Set;

const callComponentWillUnmountWithTimer = function(current, instance) {
  instance.props = current.memoizedProps;
  instance.state = current.memoizedState;
  if (
    enableProfilerTimer &&
    enableProfilerCommitHooks &&
    current.mode & ProfileMode
  ) {
    try {
      startLayoutEffectTimer();
      instance.componentWillUnmount();
    } finally {
      recordLayoutEffectDuration(current);
    }
  } else {
    instance.componentWillUnmount();
  }
};

// Capture errors so they don't interrupt unmounting.
function safelyCallComponentWillUnmount(
  current: Fiber,
  instance: any,
  nearestMountedAncestor: Fiber | null,
) {
  if (__DEV__) {
    invokeGuardedCallback(
      null,
      callComponentWillUnmountWithTimer,
      null,
      current,
      instance,
    );
    if (hasCaughtError()) {
      const unmountError = clearCaughtError();
      captureCommitPhaseError(current, nearestMountedAncestor, unmountError);
    }
  } else {
    try {
      callComponentWillUnmountWithTimer(current, instance);
    } catch (unmountError) {
      captureCommitPhaseError(current, nearestMountedAncestor, unmountError);
    }
  }
}

function safelyDetachRef(current: Fiber, nearestMountedAncestor: Fiber | null) {
  const ref = current.ref;
  if (ref !== null) {
    if (typeof ref === 'function') {
      if (__DEV__) {
        invokeGuardedCallback(null, ref, null, null);
        if (hasCaughtError()) {
          const refError = clearCaughtError();
          captureCommitPhaseError(current, nearestMountedAncestor, refError);
        }
      } else {
        try {
          ref(null);
        } catch (refError) {
          captureCommitPhaseError(current, nearestMountedAncestor, refError);
        }
      }
    } else {
      ref.current = null;
    }
  }
}

function safelyCallDestroy(
  current: Fiber,
  nearestMountedAncestor: Fiber | null,
  destroy: () => void,
) {
  if (__DEV__) {
    invokeGuardedCallback(null, destroy, null);
    if (hasCaughtError()) {
      const error = clearCaughtError();
      captureCommitPhaseError(current, nearestMountedAncestor, error);
    }
  } else {
    try {
      destroy();
    } catch (error) {
      captureCommitPhaseError(current, nearestMountedAncestor, error);
    }
  }
}

function commitBeforeMutationLifeCycles(
  current: Fiber | null,
  finishedWork: Fiber,
): void {
  switch (finishedWork.tag) {
    case FunctionComponent:
    case ForwardRef:
    case SimpleMemoComponent:
    case Block: {
      return;
    }
    case ClassComponent: {
      if (finishedWork.effectTag & Snapshot) {
        if (current !== null) {
          const prevProps = current.memoizedProps;
          const prevState = current.memoizedState;
          const instance = finishedWork.stateNode;
          // We could update instance props and state here,
          // but instead we rely on them being set during last render.
          // TODO: revisit this when we implement resuming.
          if (__DEV__) {
            if (
              finishedWork.type === finishedWork.elementType &&
              !didWarnAboutReassigningProps
            ) {
              if (instance.props !== finishedWork.memoizedProps) {
                console.error(
                  'Expected %s props to match memoized props before ' +
                    'getSnapshotBeforeUpdate. ' +
                    'This might either be because of a bug in React, or because ' +
                    'a component reassigns its own `this.props`. ' +
                    'Please file an issue.',
                  getComponentName(finishedWork.type) || 'instance',
                );
              }
              if (instance.state !== finishedWork.memoizedState) {
                console.error(
                  'Expected %s state to match memoized state before ' +
                    'getSnapshotBeforeUpdate. ' +
                    'This might either be because of a bug in React, or because ' +
                    'a component reassigns its own `this.state`. ' +
                    'Please file an issue.',
                  getComponentName(finishedWork.type) || 'instance',
                );
              }
            }
          }
          const snapshot = instance.getSnapshotBeforeUpdate(
            finishedWork.elementType === finishedWork.type
              ? prevProps
              : resolveDefaultProps(finishedWork.type, prevProps),
            prevState,
          );
          if (__DEV__) {
            const didWarnSet = ((didWarnAboutUndefinedSnapshotBeforeUpdate: any): Set<mixed>);
            if (snapshot === undefined && !didWarnSet.has(finishedWork.type)) {
              didWarnSet.add(finishedWork.type);
              console.error(
                '%s.getSnapshotBeforeUpdate(): A snapshot value (or null) ' +
                  'must be returned. You have returned undefined.',
                getComponentName(finishedWork.type),
              );
            }
          }
          instance.__reactInternalSnapshotBeforeUpdate = snapshot;
        }
      }
      return;
    }
    case HostRoot: {
      if (supportsMutation) {
        if (finishedWork.effectTag & Snapshot) {
          const root = finishedWork.stateNode;
          clearContainer(root.containerInfo);
        }
      }
      return;
    }
    case HostComponent:
    case HostText:
    case HostPortal:
    case IncompleteClassComponent:
      // Nothing to do for these component types
      return;
  }
  invariant(
    false,
    'This unit of work tag should not have side-effects. This error is ' +
      'likely caused by a bug in React. Please file an issue.',
  );
}

function commitHookEffectListUnmount(tag: number, finishedWork: Fiber) {
  const updateQueue: FunctionComponentUpdateQueue | null = (finishedWork.updateQueue: any);
  const lastEffect = updateQueue !== null ? updateQueue.lastEffect : null;
  if (lastEffect !== null) {
    const firstEffect = lastEffect.next;
    let effect = firstEffect;
    do {
      if ((effect.tag & tag) === tag) {
        // Unmount
        const destroy = effect.destroy;
        effect.destroy = undefined;
        if (destroy !== undefined) {
          destroy();
        }
      }
      effect = effect.next;
    } while (effect !== firstEffect);
  }
}

function commitHookEffectListMount(tag: number, finishedWork: Fiber) {
  const updateQueue: FunctionComponentUpdateQueue | null = (finishedWork.updateQueue: any);
  const lastEffect = updateQueue !== null ? updateQueue.lastEffect : null;
  if (lastEffect !== null) {
    const firstEffect = lastEffect.next;
    let effect = firstEffect;
    do {
      if ((effect.tag & tag) === tag) {
        // Mount
        const create = effect.create;
        effect.destroy = create();

        if (__DEV__) {
          const destroy = effect.destroy;
          if (destroy !== undefined && typeof destroy !== 'function') {
            let addendum;
            if (destroy === null) {
              addendum =
                ' You returned null. If your effect does not require clean ' +
                'up, return undefined (or nothing).';
            } else if (typeof destroy.then === 'function') {
              addendum =
                '\n\nIt looks like you wrote useEffect(async () => ...) or returned a Promise. ' +
                'Instead, write the async function inside your effect ' +
                'and call it immediately:\n\n' +
                'useEffect(() => {\n' +
                '  async function fetchData() {\n' +
                '    // You can await here\n' +
                '    const response = await MyAPI.getData(someId);\n' +
                '    // ...\n' +
                '  }\n' +
                '  fetchData();\n' +
                `}, [someId]); // Or [] if effect doesn't need props or state\n\n` +
                'Learn more about data fetching with Hooks: https://reactjs.org/link/hooks-data-fetching';
            } else {
              addendum = ' You returned: ' + destroy;
            }
            console.error(
              'An effect function must not return anything besides a function, ' +
                'which is used for clean-up.%s',
              addendum,
            );
          }
        }
      }
      effect = effect.next;
    } while (effect !== firstEffect);
  }
}

function schedulePassiveEffects(finishedWork: Fiber) {
  const updateQueue: FunctionComponentUpdateQueue | null = (finishedWork.updateQueue: any);
  const lastEffect = updateQueue !== null ? updateQueue.lastEffect : null;
  if (lastEffect !== null) {
    const firstEffect = lastEffect.next;
    let effect = firstEffect;
    do {
      const {next, tag} = effect;
      if (
        (tag & HookPassive) !== NoHookEffect &&
        (tag & HookHasEffect) !== NoHookEffect
      ) {
        enqueuePendingPassiveHookEffectUnmount(finishedWork, effect);
        enqueuePendingPassiveHookEffectMount(finishedWork, effect);
      }
      effect = next;
    } while (effect !== firstEffect);
  }
}

export function commitPassiveEffectDurations(
  finishedRoot: FiberRoot,
  finishedWork: Fiber,
): void {
  if (enableProfilerTimer && enableProfilerCommitHooks) {
    // Only Profilers with work in their subtree will have an Update effect scheduled.
    if ((finishedWork.effectTag & Update) !== NoEffect) {
      switch (finishedWork.tag) {
        case Profiler: {
          const {passiveEffectDuration} = finishedWork.stateNode;
          const {id, onPostCommit} = finishedWork.memoizedProps;

          // This value will still reflect the previous commit phase.
          // It does not get reset until the start of the next commit phase.
          const commitTime = getCommitTime();

          if (typeof onPostCommit === 'function') {
            if (enableSchedulerTracing) {
              onPostCommit(
                id,
                finishedWork.alternate === null ? 'mount' : 'update',
                passiveEffectDuration,
                commitTime,
                finishedRoot.memoizedInteractions,
              );
            } else {
              onPostCommit(
                id,
                finishedWork.alternate === null ? 'mount' : 'update',
                passiveEffectDuration,
                commitTime,
              );
            }
          }

          // Bubble times to the next nearest ancestor Profiler.
          // After we process that Profiler, we'll bubble further up.
          let parentFiber = finishedWork.return;
          while (parentFiber !== null) {
            if (parentFiber.tag === Profiler) {
              const parentStateNode = parentFiber.stateNode;
              parentStateNode.passiveEffectDuration += passiveEffectDuration;
              break;
            }
            parentFiber = parentFiber.return;
          }
          break;
        }
        default:
          break;
      }
    }
  }
}

function commitLifeCycles(
  finishedRoot: FiberRoot,
  current: Fiber | null,
  finishedWork: Fiber,
  committedLanes: Lanes,
): void {
  switch (finishedWork.tag) {
    case FunctionComponent:
    case ForwardRef:
    case SimpleMemoComponent:
    case Block: {
      // At this point layout effects have already been destroyed (during mutation phase).
      // This is done to prevent sibling component effects from interfering with each other,
      // e.g. a destroy function in one component should never override a ref set
      // by a create function in another component during the same commit.
      if (
        enableProfilerTimer &&
        enableProfilerCommitHooks &&
        finishedWork.mode & ProfileMode
      ) {
        try {
          startLayoutEffectTimer();
          commitHookEffectListMount(HookLayout | HookHasEffect, finishedWork);
        } finally {
          recordLayoutEffectDuration(finishedWork);
        }
      } else {
        commitHookEffectListMount(HookLayout | HookHasEffect, finishedWork);
      }

      schedulePassiveEffects(finishedWork);
      return;
    }
    case ClassComponent: {
      const instance = finishedWork.stateNode;
      if (finishedWork.effectTag & Update) {
        if (current === null) {
          // We could update instance props and state here,
          // but instead we rely on them being set during last render.
          // TODO: revisit this when we implement resuming.
          if (__DEV__) {
            if (
              finishedWork.type === finishedWork.elementType &&
              !didWarnAboutReassigningProps
            ) {
              if (instance.props !== finishedWork.memoizedProps) {
                console.error(
                  'Expected %s props to match memoized props before ' +
                    'componentDidMount. ' +
                    'This might either be because of a bug in React, or because ' +
                    'a component reassigns its own `this.props`. ' +
                    'Please file an issue.',
                  getComponentName(finishedWork.type) || 'instance',
                );
              }
              if (instance.state !== finishedWork.memoizedState) {
                console.error(
                  'Expected %s state to match memoized state before ' +
                    'componentDidMount. ' +
                    'This might either be because of a bug in React, or because ' +
                    'a component reassigns its own `this.state`. ' +
                    'Please file an issue.',
                  getComponentName(finishedWork.type) || 'instance',
                );
              }
            }
          }
          if (
            enableProfilerTimer &&
            enableProfilerCommitHooks &&
            finishedWork.mode & ProfileMode
          ) {
            try {
              startLayoutEffectTimer();
              instance.componentDidMount();
            } finally {
              recordLayoutEffectDuration(finishedWork);
            }
          } else {
            instance.componentDidMount();
          }
        } else {
          const prevProps =
            finishedWork.elementType === finishedWork.type
              ? current.memoizedProps
              : resolveDefaultProps(finishedWork.type, current.memoizedProps);
          const prevState = current.memoizedState;
          // We could update instance props and state here,
          // but instead we rely on them being set during last render.
          // TODO: revisit this when we implement resuming.
          if (__DEV__) {
            if (
              finishedWork.type === finishedWork.elementType &&
              !didWarnAboutReassigningProps
            ) {
              if (instance.props !== finishedWork.memoizedProps) {
                console.error(
                  'Expected %s props to match memoized props before ' +
                    'componentDidUpdate. ' +
                    'This might either be because of a bug in React, or because ' +
                    'a component reassigns its own `this.props`. ' +
                    'Please file an issue.',
                  getComponentName(finishedWork.type) || 'instance',
                );
              }
              if (instance.state !== finishedWork.memoizedState) {
                console.error(
                  'Expected %s state to match memoized state before ' +
                    'componentDidUpdate. ' +
                    'This might either be because of a bug in React, or because ' +
                    'a component reassigns its own `this.state`. ' +
                    'Please file an issue.',
                  getComponentName(finishedWork.type) || 'instance',
                );
              }
            }
          }
          if (
            enableProfilerTimer &&
            enableProfilerCommitHooks &&
            finishedWork.mode & ProfileMode
          ) {
            try {
              startLayoutEffectTimer();
              instance.componentDidUpdate(
                prevProps,
                prevState,
                instance.__reactInternalSnapshotBeforeUpdate,
              );
            } finally {
              recordLayoutEffectDuration(finishedWork);
            }
          } else {
            instance.componentDidUpdate(
              prevProps,
              prevState,
              instance.__reactInternalSnapshotBeforeUpdate,
            );
          }
        }
      }

      // TODO: I think this is now always non-null by the time it reaches the
      // commit phase. Consider removing the type check.
      const updateQueue: UpdateQueue<
        *,
      > | null = (finishedWork.updateQueue: any);
      if (updateQueue !== null) {
        if (__DEV__) {
          if (
            finishedWork.type === finishedWork.elementType &&
            !didWarnAboutReassigningProps
          ) {
            if (instance.props !== finishedWork.memoizedProps) {
              console.error(
                'Expected %s props to match memoized props before ' +
                  'processing the update queue. ' +
                  'This might either be because of a bug in React, or because ' +
                  'a component reassigns its own `this.props`. ' +
                  'Please file an issue.',
                getComponentName(finishedWork.type) || 'instance',
              );
            }
            if (instance.state !== finishedWork.memoizedState) {
              console.error(
                'Expected %s state to match memoized state before ' +
                  'processing the update queue. ' +
                  'This might either be because of a bug in React, or because ' +
                  'a component reassigns its own `this.state`. ' +
                  'Please file an issue.',
                getComponentName(finishedWork.type) || 'instance',
              );
            }
          }
        }
        // We could update instance props and state here,
        // but instead we rely on them being set during last render.
        // TODO: revisit this when we implement resuming.
        commitUpdateQueue(finishedWork, updateQueue, instance);
      }
      return;
    }
    case HostRoot: {
      // TODO: I think this is now always non-null by the time it reaches the
      // commit phase. Consider removing the type check.
      const updateQueue: UpdateQueue<
        *,
      > | null = (finishedWork.updateQueue: any);
      if (updateQueue !== null) {
        let instance = null;
        if (finishedWork.child !== null) {
          switch (finishedWork.child.tag) {
            case HostComponent:
              instance = getPublicInstance(finishedWork.child.stateNode);
              break;
            case ClassComponent:
              instance = finishedWork.child.stateNode;
              break;
          }
        }
        commitUpdateQueue(finishedWork, updateQueue, instance);
      }
      return;
    }
    case HostComponent: {
      const instance: Instance = finishedWork.stateNode;

      // Renderers may schedule work to be done after host components are mounted
      // (eg DOM renderer may schedule auto-focus for inputs and form controls).
      // These effects should only be committed when components are first mounted,
      // aka when there is no current/alternate.
      if (current === null && finishedWork.effectTag & Update) {
        const type = finishedWork.type;
        const props = finishedWork.memoizedProps;
        commitMount(instance, type, props, finishedWork);
      }

      return;
    }
    case HostText: {
      // We have no life-cycles associated with text.
      return;
    }
    case HostPortal: {
      // We have no life-cycles associated with portals.
      return;
    }
    case Profiler: {
      if (enableProfilerTimer) {
        const {onCommit, onRender} = finishedWork.memoizedProps;
        const {effectDuration} = finishedWork.stateNode;

        const commitTime = getCommitTime();

        if (typeof onRender === 'function') {
          if (enableSchedulerTracing) {
            onRender(
              finishedWork.memoizedProps.id,
              current === null ? 'mount' : 'update',
              finishedWork.actualDuration,
              finishedWork.treeBaseDuration,
              finishedWork.actualStartTime,
              commitTime,
              finishedRoot.memoizedInteractions,
            );
          } else {
            onRender(
              finishedWork.memoizedProps.id,
              current === null ? 'mount' : 'update',
              finishedWork.actualDuration,
              finishedWork.treeBaseDuration,
              finishedWork.actualStartTime,
              commitTime,
            );
          }
        }

        if (enableProfilerCommitHooks) {
          if (typeof onCommit === 'function') {
            if (enableSchedulerTracing) {
              onCommit(
                finishedWork.memoizedProps.id,
                current === null ? 'mount' : 'update',
                effectDuration,
                commitTime,
                finishedRoot.memoizedInteractions,
              );
            } else {
              onCommit(
                finishedWork.memoizedProps.id,
                current === null ? 'mount' : 'update',
                effectDuration,
                commitTime,
              );
            }
          }

          // Schedule a passive effect for this Profiler to call onPostCommit hooks.
          // This effect should be scheduled even if there is no onPostCommit callback for this Profiler,
          // because the effect is also where times bubble to parent Profilers.
          enqueuePendingPassiveProfilerEffect(finishedWork);

          // Propagate layout effect durations to the next nearest Profiler ancestor.
          // Do not reset these values until the next render so DevTools has a chance to read them first.
          let parentFiber = finishedWork.return;
          while (parentFiber !== null) {
            if (parentFiber.tag === Profiler) {
              const parentStateNode = parentFiber.stateNode;
              parentStateNode.effectDuration += effectDuration;
              break;
            }
            parentFiber = parentFiber.return;
          }
        }
      }
      return;
    }
    case SuspenseComponent: {
      commitSuspenseHydrationCallbacks(finishedRoot, finishedWork);
      return;
    }
    case SuspenseListComponent:
    case IncompleteClassComponent:
    case FundamentalComponent:
    case ScopeComponent:
    case OffscreenComponent:
    case LegacyHiddenComponent:
      return;
  }
  invariant(
    false,
    'This unit of work tag should not have side-effects. This error is ' +
      'likely caused by a bug in React. Please file an issue.',
  );
}

function hideOrUnhideAllChildren(finishedWork, isHidden) {
  if (supportsMutation) {
    // We only have the top Fiber that was inserted but we need to recurse down its
    // children to find all the terminal nodes.
    let node: Fiber = finishedWork;
    while (true) {
      if (node.tag === HostComponent) {
        const instance = node.stateNode;
        if (isHidden) {
          hideInstance(instance);
        } else {
          unhideInstance(node.stateNode, node.memoizedProps);
        }
      } else if (node.tag === HostText) {
        const instance = node.stateNode;
        if (isHidden) {
          hideTextInstance(instance);
        } else {
          unhideTextInstance(instance, node.memoizedProps);
        }
      } else if (
        (node.tag === OffscreenComponent ||
          node.tag === LegacyHiddenComponent) &&
        (node.memoizedState: OffscreenState) !== null &&
        node !== finishedWork
      ) {
        // Found a nested Offscreen component that is hidden. Don't search
        // any deeper. This tree should remain hidden.
      } else if (node.child !== null) {
        node.child.return = node;
        node = node.child;
        continue;
      }
      if (node === finishedWork) {
        return;
      }
      while (node.sibling === null) {
        if (node.return === null || node.return === finishedWork) {
          return;
        }
        node = node.return;
      }
      node.sibling.return = node.return;
      node = node.sibling;
    }
  }
}

function commitAttachRef(finishedWork: Fiber) {
  const ref = finishedWork.ref;
  if (ref !== null) {
    const instance = finishedWork.stateNode;
    let instanceToUse;
    switch (finishedWork.tag) {
      case HostComponent:
        instanceToUse = getPublicInstance(instance);
        break;
      default:
        instanceToUse = instance;
    }
    // Moved outside to ensure DCE works with this flag
    if (enableScopeAPI && finishedWork.tag === ScopeComponent) {
      instanceToUse = instance;
    }
    if (typeof ref === 'function') {
      ref(instanceToUse);
    } else {
      if (__DEV__) {
        if (!ref.hasOwnProperty('current')) {
          console.error(
            'Unexpected ref object provided for %s. ' +
              'Use either a ref-setter function or React.createRef().',
            getComponentName(finishedWork.type),
          );
        }
      }

      ref.current = instanceToUse;
    }
  }
}

function commitDetachRef(current: Fiber) {
  const currentRef = current.ref;
  if (currentRef !== null) {
    if (typeof currentRef === 'function') {
      currentRef(null);
    } else {
      currentRef.current = null;
    }
  }
}




    
