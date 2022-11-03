<TeXmacs|2.1.1>

<style|article>

<\body>
  <doc-data|<doc-title|Guide to the Compiler>|<doc-date|April 27,
  2022>|<doc-author|<author-data|<author-name|Ameer Qaqish>>>>

  <section|Scope of the Project>

  \;

  The project successfully implements all features of PA1 through PA4. It
  does not implement any optional parts.

  <section|Summary of Changes to AST Classes>

  <subsection|For Type Checking and Identification>

  \;

  1. Added NullLiteral AST class and corresponding method "public ResultType
  visitNullLiteral(NullLiteral nullLiteral, ArgType arg);" to the Visitor
  interface.

  \;

  2. Added \P<code|Declaration decl;>\Q field in the Identifier class and
  Reference class. This field holds the Declaration that the object refers
  to.

  \;

  3. Modified the AST Displayer to implement the newly added method in the
  Visitor interface.

  \;

  4. Added "Typedenoter type;" field to the AST class

  \;

  5. Added implementations of toString method in the implementations of
  TypeDenoter.

  \;

  6. Added implementation of "equals" method for the TypeDenoter class.

  \;

  7. Added \P<code|MethodDecl methodDecl;>\Q field to ReturnStmt class. This
  field stores the method in which the return statement was called.

  <subsection|For Code Generation>

  \;

  8. Added "RuntimeDescription runtimeDescription;" field to Declaration
  class. RuntimeDescription is a class in the CodeGenerator package.
  Instances of RuntimeDescription are attatched to declarations and keep
  track of the variable's size and location in memory.

  \;

  9. Added \P<code|MethodDecl mainDecl;>\Q field to Package class. This field
  holds the delcaration of the main method of the program. \ 
</body>

<\initial>
  <\collection>
    <associate|font-base-size|12>
  </collection>
</initial>

<\references>
  <\collection>
    <associate|auto-1|<tuple|1|?>>
    <associate|auto-2|<tuple|2|?>>
    <associate|auto-3|<tuple|2.1|?>>
    <associate|auto-4|<tuple|2.2|?>>
  </collection>
</references>

<\auxiliary>
  <\collection>
    <\associate|toc>
      <vspace*|1fn><with|font-series|<quote|bold>|math-font-series|<quote|bold>|1<space|2spc>>
      <datoms|<macro|x|<repeat|<arg|x>|<with|font-series|medium|<with|font-size|1|<space|0.2fn>.<space|0.2fn>>>>>|<htab|5mm>>
      <no-break><pageref|auto-1><vspace|0.5fn>
    </associate>
  </collection>
</auxiliary>