SELECT
path
--,filetype,replication,blocksize,mtime,atime,permission,username,groupname
--,LENGTH(content) AS length
--, SUBSTR(path,0,34) AS onemore
--, INSTR(SUBSTR(path, 34),'/') AS slashmore
FROM filesystem
 WHERE
 (path GLOB "/fr/resource/SmithMark/.785.html/*")
 AND NOT (path GLOB "/fr/resource/SmithMark/.785.html/*/*")
 --AND LENGTH(path) >= 34
 --AND SUBSTR(path,0,34) = "/fr/resource/SmithMark/.785.html/"
 --AND INSTR(SUBSTR(path, 34),'/')=0
 --LIMIT 10
