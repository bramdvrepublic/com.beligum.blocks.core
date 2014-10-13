#IDs
##URI and URL
- URI's are used for (uniquely) identifying an page, row, block, ... be it in the application-cache or in the database.
- URL's are used to represent 'visitable' resources, things that can actually (almost one-to-one) be seen in a client-browser.

##Database
URI's are used of the form "site-alias/page-name#element-id:version"

##Cacher
URI's are used of the form "page-name#element-id"

##Examples
- "page/waterput#row1" is a URI to the cached value of "row1" of the "waterput"-template
- "MOT/default#row1:1412858435650" is a URI to the content of this row saved in db 
- "http://www.mot.be/default/123321321#row1" is the URL which a browser can go to for visiting that row




"http://www.mot.be/mooie-paginas/pagina-1"
"http://www.mot.be/mooie-paginas/pagina-1?v=4000"
"http://www.mot.be/mooie-paginas/pagina-1/row/bla"
"http://www.mot.be/mooie-paginas/pagina-1#bla"


"MOT/default#row1:1234" -> versie van row1
"MOT/default

MOT/mooie-pagina (pag class = default)
MOT/mooie-pagina (page class = waterput)