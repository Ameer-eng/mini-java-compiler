  0         PUSH         1
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   LOADL        -1
  6         LOADL        1
  7         CALL         newobj  
  8         LOAD         3[LB]
  9         CALLI        L11
 10         LOADL        1
 11         CALL         neg     
 12         LOAD         3[LB]
 13         LOADL        0
 14         CALL         fieldref
 15         CALLI        L12
 16         RETURN (0)   1
 17  L11:   LOADA        0[OB]
 18         STORE        0[OB]
 19         RETURN (0)   0
 20  L12:   LOAD         -1[LB]
 21         LOADL        27
 22         CALL         add     
 23         CALL         putintnl
 24         RETURN (0)   1
 25         RETURN (0)   1
