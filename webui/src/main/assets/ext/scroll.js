if (!window.__wx__webview_set) {
  window.__wx__webview_set = true;

  const DEBUG = webui.isDebug();
  let state = {
    prevented: false,
    awaitingResponse: false,
    touchElement: null,
    mutatedWhileTouch: false
  };

  // Helper functions
  const isAtEdge = (element, direction) => {
    if (!element || element === document) return true;
    const prop = direction === 'left' ? 'scrollLeft' : 'scrollTop';
    return element[prop] <= 0 && isAtEdge(element.parentNode, direction);
  };

  const updateScrollState = (target) => {
    if (!window.WebUIXView || !state.awaitingResponse) return;

    const scrolledToTop = target.scrollTop === 0 &&
      (!window.visualViewport || window.visualViewport.offsetTop === 0);

    if (DEBUG) {
      console.log('WebUI X scroll state updated:', { scrolledToTop, prevented: state.prevented });
    }

    webui.setRefreshingEnabled(scrolledToTop);
    state.awaitingResponse = false;
    state.prevented = false;
    state.mutatedWhileTouch = false;
  };

  // Touch event listeners
  document.addEventListener('touchstart', (e) => {
    state.touchElement = e.target;
    state.awaitingResponse = true;
  }, true);

  document.addEventListener('touchmove', (e) => {
    if (state.awaitingResponse) {
      setTimeout(() => updateScrollState(e.target), 16);
    }
  }, true);

  document.addEventListener('scroll', (e) => {
    if (e.target) {
      updateScrollState(e.target);
    }
  }, true);

  // Override preventDefault to track when scrolling is prevented
  if (TouchEvent) {
    const originalPreventDefault = TouchEvent.prototype.preventDefault;
    TouchEvent.prototype.preventDefault = function() {
      state.prevented = true;
      originalPreventDefault.call(this);
    };
  }

  // Track DOM mutations during touch
  const isChildOf = (child, parent) => {
    while (child && child !== parent) {
      child = child.parentElement;
    }
    return child === parent;
  };

  new MutationObserver((mutations) => {
    if (!state.touchElement) return;

    const hasRelevantMutation = mutations.some(mutation =>
      (mutation.attributeName === 'style' || mutation.attributeName === 'class') &&
      mutation.target !== document.body &&
      mutation.target !== document.documentElement &&
      isChildOf(state.touchElement, mutation.target)
    );

    if (hasRelevantMutation) {
      if (DEBUG) console.log('WebUI X mutation detected');
      state.mutatedWhileTouch = true;
    }
  }).observe(document, { attributes: true, childList: true, subtree: true });
}