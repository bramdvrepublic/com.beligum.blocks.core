# Javascript notes
* all starts in menu.js by checking for a cookie to slide open the sidebar or just by a click on the main icon that slides open the sidebar
* first event that's launched is START_BLOCK, after the sidebar's animation is finished (just like the last event will be STOP_BLOCKS)
* the immediate result is that the Broadcaster's 'active' flag rises (allowing for a range of things to happen there)
* the event is sent out and received by manager.js


## Important events: 
* DO_REFRESH_LAYOUT: is for when the we have to rebuild the layout tree, but the dom did not change (e.g. window resize)
    * this is only implemented in manager.js, but sent through using WILL_REFRESH_LAYOUT to menu.js so that the container size is first updated

* DOM_CHANGED: when the contents of the DOM changed (blocks etc added/removed/resized/...); note that this fires DO_REFRESH_LAYOUT in the end, which is far more important
* ACTIVATE_MOUSE: (de)activates the mouse cf. pauzes the templates layouter (used during dialogs, resizing, etc)

* Broadcaster.getHoveredBlockForPosition()
    * used in: 
        * Broadcaster.registerMouseMove()
        * Broadcaster.buildLayoutTree()
        * DragDrop.dragStarted()
        * Mouse.mousedown()
* Broadcaster.resetHover()
* Mouse.activate() -> mousemove
* Resizer.startDrag

## Files
* layout.js: handles everything of the DnD and moving of the blocks
* dragdrop.js: handles the visual indicators for the logic in layout.js
* 
