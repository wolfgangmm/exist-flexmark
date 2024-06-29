xquery version "3.1";

(:~
 : A very simple example XQuery Library Module implemented
 : in XQuery.
 :)
module namespace md = "https://e-editiones.org/exist-db/markdown";

declare function md:say-hello($name as xs:string?) as document-node(element(hello)) {
    document {
        element hello {
            if($name) then
                $name
            else
                "stranger"
        }
    }
};

declare function md:hello-world() as document-node(element(hello)) {
    md:say-hello("world")
};

declare function md:add($a as xs:int, $b as xs:int) as xs:int {
    $a + $b
};