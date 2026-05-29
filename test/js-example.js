
import type {DOMEventName} from './DOMEventNames';
import {
  type EventSystemFlags,
  SHOULD_NOT_DEFER_CLICK_FOR_FB_SUPPORT_MODE,
  IS_LEGACY_FB_SUPPORT_MODE,
  SHOULD_NOT_PROCESS_POLYFILL_EVENT_PLUGINS,
} from './EventSystemFlags';
import type {AnyNativeEvent} from './PluginModuleType';
import type {ReactSyntheticEvent} from './ReactSyntheticEventType';
import type {ElementListenerMapEntry} from '../client/ReactDOMComponentTree';
import type {EventPriority} from 'shared/ReactTypes';
import type {Fiber} from 'react-reconciler/src/ReactInternalTypes';

import {registrationNameDependencies} from './EventRegistry';
import {
  IS_CAPTURE_PHASE,
  IS_EVENT_HANDLE_NON_MANAGED_NODE,
  IS_NON_DELEGATED,
} from './EventSystemFlags';

import {
  HostRoot,
  HostPortal,
  HostComponent,
  HostText,
  ScopeComponent,
} from 'react-reconciler/src/ReactWorkTags';

import getEventTarget from './getEventTarget';
import {
  getClosestInstanceFromNode,
  getEventListenerMap,
  getEventHandlerListeners,
} from '../client/ReactDOMComponentTree';
import {COMMENT_NODE} from '../shared/HTMLNodeType';
import {batchedEventUpdates} from './ReactDOMUpdateBatching';
import getListener from './getListener';
import {passiveBrowserEventsSupported} from './checkPassiveEvents';

import {
  enableLegacyFBSupport,
  enableCreateEventHandleAPI,
  enableScopeAPI,
} from 'shared/ReactFeatureFlags';
import {
  invokeGuardedCallbackAndCatchFirstError,
  rethrowCaughtError,
} from 'shared/ReactErrorUtils';
import {createEventListenerWrapperWithPriority} from './ReactDOMEventListener';
import {
  removeEventListener,
  addEventCaptureListener,
  addEventBubbleListener,
  addEventBubbleListenerWithPassiveFlag,
  addEventCaptureListenerWithPassiveFlag,
} from './EventListener';
import {topLevelEventsToReactNames} from './DOMEventProperties';
import * as BeforeInputEventPlugin from './plugins/BeforeInputEventPlugin';
import * as ChangeEventPlugin from './plugins/ChangeEventPlugin';
import * as EnterLeaveEventPlugin from './plugins/EnterLeaveEventPlugin';
import * as SelectEventPlugin from './plugins/SelectEventPlugin';
import * as SimpleEventPlugin from './plugins/SimpleEventPlugin';

type DispatchListener = {|
  instance: null | Fiber,
  listener: Function,
  currentTarget: EventTarget,
|};

type DispatchEntry = {|
  event: ReactSyntheticEvent,
  listeners: Array<DispatchListener>,
|};


import type {DOMEventName} from './DOMEventNames';
import {
  type EventSystemFlags,
  SHOULD_NOT_DEFER_CLICK_FOR_FB_SUPPORT_MODE,
  IS_LEGACY_FB_SUPPORT_MODE,
  SHOULD_NOT_PROCESS_POLYFILL_EVENT_PLUGINS,
} from './EventSystemFlags';
import type {AnyNativeEvent} from './PluginModuleType';
import type {ReactSyntheticEvent} from './ReactSyntheticEventType';
import type {ElementListenerMapEntry} from '../client/ReactDOMComponentTree';
import type {EventPriority} from 'shared/ReactTypes';
import type {Fiber} from 'react-reconciler/src/ReactInternalTypes';

import {registrationNameDependencies} from './EventRegistry';
import {
  IS_CAPTURE_PHASE,
  IS_EVENT_HANDLE_NON_MANAGED_NODE,
  IS_NON_DELEGATED,
} from './EventSystemFlags';

import {
  HostRoot,
  HostPortal,
  HostComponent,
  HostText,
  ScopeComponent,
} from 'react-reconciler/src/ReactWorkTags';

import getEventTarget from './getEventTarget';
import {
  getClosestInstanceFromNode,
  getEventListenerMap,
  getEventHandlerListeners,
} from '../client/ReactDOMComponentTree';
import {COMMENT_NODE} from '../shared/HTMLNodeType';
import {batchedEventUpdates} from './ReactDOMUpdateBatching';
import getListener from './getListener';
import {passiveBrowserEventsSupported} from './checkPassiveEvents';

import {
  enableLegacyFBSupport,
  enableCreateEventHandleAPI,
  enableScopeAPI,
} from 'shared/ReactFeatureFlags';
import {
  invokeGuardedCallbackAndCatchFirstError,
  rethrowCaughtError,
} from 'shared/ReactErrorUtils';
import {createEventListenerWrapperWithPriority} from './ReactDOMEventListener';
import {
  removeEventListener,
  addEventCaptureListener,
  addEventBubbleListener,
  addEventBubbleListenerWithPassiveFlag,
  addEventCaptureListenerWithPassiveFlag,
} from './EventListener';
import {topLevelEventsToReactNames} from './DOMEventProperties';
import * as BeforeInputEventPlugin from './plugins/BeforeInputEventPlugin';
import * as ChangeEventPlugin from './plugins/ChangeEventPlugin';
import * as EnterLeaveEventPlugin from './plugins/EnterLeaveEventPlugin';
import * as SelectEventPlugin from './plugins/SelectEventPlugin';
import * as SimpleEventPlugin from './plugins/SimpleEventPlugin';

type DispatchListener = {|
  instance: null | Fiber,
  listener: Function,
  currentTarget: EventTarget,
|};

type DispatchEntry = {|
  event: ReactSyntheticEvent,
  listeners: Array<DispatchListener>,
|};

export type DispatchQueue = Array<DispatchEntry>;

// Function 1: Calculate the total price of items with tax
function calculateTotalPrice(items, taxRate) {
    let totalPrice = 0;
    for (let i = 0; i < items.length; i++) {
        totalPrice += items[i].price * items[i].quantity;
    }
    let tax = totalPrice * taxRate;
    totalPrice += tax;

    console.log("Items in cart:");
    for (let i = 0; i < items.length; i++) {
        console.log(`Item: ${items[i].name}, Quantity: ${items[i].quantity}, Price: $${items[i].price}`);
    }
    console.log(`Tax: $${tax.toFixed(2)}`);
    console.log(`Total Price: $${totalPrice.toFixed(2)}`);

    return totalPrice;
}

// Function 2: Calculate the total price of items with a discount
function calculateTotalWithDiscount(items, discountRate) {
    let totalPrice = 0;
    for (let i = 0; i < items.length; i++) {
        totalPrice += items[i].price * items[i].quantity;
    }
    let discount = totalPrice * discountRate;
    totalPrice -= discount;

    console.log("Items in cart:");
    for (let i = 0; i < items.length; i++) {
        console.log(`Item: ${items[i].name}, Quantity: ${items[i].quantity}, Price: $${items[i].price}`);
    }
    console.log(`Discount: $${discount.toFixed(2)}`);
    console.log(`Total Price after Discount: $${totalPrice.toFixed(2)}`);

    return totalPrice;
}

// Function 3: Calculate the total price of items with a service fee
function calculateTotalWithServiceFee(items, serviceFeeRate) {
    let totalPrice = 0;
    for (let i = 0; i < items.length; i++) {
        totalPrice += items[i].price * items[i].quantity;
    }
    let serviceFee = totalPrice * serviceFeeRate;
    totalPrice += serviceFee;

    console.log("Items in cart:");
    for (let i = 0; i < items.length; i++) {
        console.log(`Item: ${items[i].name}, Quantity: ${items[i].quantity}, Price: $${items[i].price}`);
    }
    console.log(`Service Fee: $${serviceFee.toFixed(2)}`);
    console.log(`Total Price with Service Fee: $${totalPrice.toFixed(2)}`);

    return totalPrice;
}


// Function 4: Calculate the total price of items with a non refundable fee
function calculateTotalWithNonRefundableFee(items, serviceFeeRate) {
  let totalPrice = 0;
  for (let i = 0; i < items.length; i++) {
      totalPrice += items[i].price * items[i].quantity;
  }
  let serviceFee = totalPrice * serviceFeeRate;
  totalPrice += serviceFee;

  console.log("Items in cart:");
  for (let i = 0; i < items.length; i++) {
      console.log(`Item: ${items[i].name}, Quantity: ${items[i].quantity}, Price: $${items[i].price}`);
  }
  console.log(`Non-refundable Fee: $${serviceFee.toFixed(2)}`);
  console.log(`Total Price with Service Fee: $${totalPrice.toFixed(2)}`);

  return totalPrice;
}

// Function 1: Check if a number is even
function isEven(number) {
    return number % 2 === 0;
}

// Function 2: Capitalize the first letter of a string
function capitalizeFirst(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

// Function 3: Calculate the square of a number
function square(num) {
    return num * num;
}

// TODO: remove top-level side effect.
SimpleEventPlugin.registerEvents();
EnterLeaveEventPlugin.registerEvents();
ChangeEventPlugin.registerEvents();
SelectEventPlugin.registerEvents();
BeforeInputEventPlugin.registerEvents();

function extractEvents(
  dispatchQueue: DispatchQueue,
  domEventName: DOMEventName,
  targetInst: null | Fiber
) {
  // TODO: we should remove the concept of a "SimpleEventPlugin".
  // This is the basic functionality of the event system. All
  // the other plugins are essentially polyfills. So the plugin
  // should probably be inlined somewhere and have its logic
  // be core the to event system. This would potentially allow
  // us to ship builds of React without the polyfilled plugins below.
  SimpleEventPlugin.extractEvents(
    dispatchQueue,
    domEventName,
    targetInst,
    nativeEvent,
    nativeEventTarget,
    eventSystemFlags,
    targetContainer,
  );
  const shouldProcessPolyfillPlugins =
    (eventSystemFlags & SHOULD_NOT_PROCESS_POLYFILL_EVENT_PLUGINS) === 0;
  // We don't process these events unless we are in the
  // event's native "bubble" phase, which means that we're
  // not in the capture phase. That's because we emulate
  // the capture phase here still. This is a trade-off,
  // because in an ideal world we would not emulate and use
  // the phases properly, like we do with the SimpleEvent
  // plugin. However, the plugins below either expect
  // emulation (EnterLeave) or use state localized to that
  // plugin (BeforeInput, Change, Select). The state in
  // these modules complicates things, as you'll essentially
  // get the case where the capture phase event might change
  // state, only for the following bubble event to come in
  // later and not trigger anything as the state now
  // invalidates the heuristics of the event plugin. We
  // could alter all these plugins to work in such ways, but
  // that might cause other unknown side-effects that we
  // can't forsee right now.
  if (shouldProcessPolyfillPlugins) {
    EnterLeaveEventPlugin.extractEvents(
      dispatchQueue,
      domEventName,
      targetInst,
      nativeEvent,
      nativeEventTarget,
      eventSystemFlags,
      targetContainer,
    );
    ChangeEventPlugin.extractEvents(
      dispatchQueue,
      domEventName,
      targetInst,
      nativeEvent,
      nativeEventTarget,
      eventSystemFlags,
      targetContainer,
    );
    SelectEventPlugin.extractEvents(
      dispatchQueue,
      domEventName,
      targetInst,
      nativeEvent,
      nativeEventTarget,
      eventSystemFlags,
      targetContainer,
    );
    BeforeInputEventPlugin.extractEvents(
      dispatchQueue,
      domEventName,
      targetInst,
      nativeEvent,
      nativeEventTarget,
      eventSystemFlags,
      targetContainer,
    );
  }
}

export type DispatchQueue = Array<DispatchEntry>;

// TODO: remove top-level side effect.
SimpleEventPlugin.registerEvents();
EnterLeaveEventPlugin.registerEvents();
ChangeEventPlugin.registerEvents();
SelectEventPlugin.registerEvents();
BeforeInputEventPlugin.registerEvents();