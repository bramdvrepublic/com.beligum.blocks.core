/**
 * @license Copyright (c) 2003-2014, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

CKEDITOR.editorConfig = function( config ) {
	// Define changes to default configuration here. For example:
	// config.language = 'fr';
	// config.uiColor = '#AADC6E';
    config.toolbar = [
        { name: 'clipboard', groups: ['undo' ], items: [ 'Undo', 'Redo' ] },
        { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ], items: [ 'Bold', 'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript', '-', 'RemoveFormat' ] },

        '/',

        { name: 'paragraph', groups: [ 'list',  'align' ], items: [ 'NumberedList', 'BulletedList', '-', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock' ] },
        { name: 'links', items: [ 'Link', 'Unlink', 'Anchor' ] },

        { name: 'styles', items: [ 'Styles', 'Format' ] }
    ];

// Toolbar groups configuration.
    config.toolbarGroups = [
        { name: 'clipboard', groups: [ 'undo' ] },
        { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
        '/',

        { name: 'paragraph', groups: [ 'list', 'align'] },
        { name: 'links' },

        { name: 'styles' }
    ];

};
//
//CKEDITOR.stylesSet.add( 'default', [
//    /* Block Styles */
//
//
//
//    { name: 'Title 1',		element: 'h1' },
//    { name: 'Title 2',		element: 'h2' },
//    { name: 'Title 3',		element: 'h3' },
//    { name: 'Title 4',		element: 'h4' },
//    { name: 'Title 5',		element: 'h5' },
//    { name: 'Title 6',		element: 'h4' }
//
//] );
