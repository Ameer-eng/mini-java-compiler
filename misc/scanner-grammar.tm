<TeXmacs|2.1.2>

<style|<tuple|article|padded-paragraphs>>

<\body>
  Scanner grammar is

  <\eqnarray*>
    <tformat|<table|<row|<cell|id>|<cell|<text|::=>>|<cell|letter
    <around*|(|letter\<mid\>digit\<mid\><with|font-series|bold|_>|)>>>|<row|<cell|num>|<cell|<text|::=>>|<cell|decimal<rsup|\<ast\>>>>>>
  </eqnarray*>

  Factored Parser grammar:

  \;

  Program ::= ClassDeclaration* eot

  ClassDeclaration ::= class id { (\ 

  <space|2em>Visibility Access (

  <space|2em><space|2em>Type id (; \| <with|font-series|bold|(>
  ParameterList? <with|font-series|bold|)> {Statement*})\ 

  <space|2em><space|2em>\|\ 

  <space|2em><space|2em>void id <with|font-series|bold|(> ParameterList?
  <with|font-series|bold|)> {Statement*}

  <space|2em>)\ 

  )* }

  FieldDeclaration ::= Visibility Access Type id ;

  MethodDeclaration ::= Visibility Access (Type \| void) id
  <with|font-series|bold|(> ParameterList? <with|font-series|bold|)>
  {Statement*}

  Visibility ::= (public \| private)?

  Access ::= static ?

  Type ::= boolean \| id (<math|\<varepsilon\>> \| []) \| int
  (<math|\<varepsilon\>> \| [])

  ParameterList ::= Type id (, Type id)*

  ArgumentList ::= Expression (, Expression)*

  Reference ::= (id \| this) (. id)*

  Statement ::=\ 

  <space|4em>{ Statement* }\ 

  <space|4em>\| Type id = Expression ; \ 

  <space|4em>\| Reference (= Expression ;\ 

  <space|9em>\| [ Expression ] = Expression ;

  <space|9em>\| <with|font-series|bold|(> ArgumentList?
  <with|font-series|bold|)> ;)\ 

  <space|4em>\| return Expression? ;\ 

  <space|4em>\| if <with|font-series|bold|(> Expression
  <with|font-series|bold|><with|font-series|bold|)> Statement (else
  Statement)? \ 

  <space|4em>\| while <with|font-series|bold|(> Expression
  <with|font-series|bold|)> Statement\ 

  Expression ::= Expression# (binop Expression#)*

  Expression# ::=\ 

  <space|6em>Reference (<math|\<varepsilon\>> \| [ Expression ] \|
  <with|font-series|bold|(> ArgumentList? <with|font-series|bold|)>)

  <space|6em>\| unop Expression\ 

  <space|6em>\| <with|font-series|bold|(> Expression
  <with|font-series|bold|)>\ 

  <space|6em>\| num \| true \| false \| null

  <space|6em>\| new ( id <with|font-series|bold|()> \| int [ Expression ] \|
  id [ Expression ] )

  \;

  \;

  \;

  \;
</body>

<\initial>
  <\collection>
    <associate|font-base-size|12>
  </collection>
</initial>