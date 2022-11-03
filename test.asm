  0         PUSH         1
  1         LOADL        0
  2         CALL         newarr  
  3         CALL         L10
  4         HALT   (0)   
  5  L10:   LOADL        6
  6         LOADA        0[OB]
  7         CALLI        L11
  8         LOADL        9
  9         LOADA        0[OB]
 10         CALLI        L11
 11         CALL         mult    
 12         LOAD         3[LB]
 13         CALL         putintnl
 14         RETURN (0)   1
 15  L11:   LOAD         -1[LB]
 16         LOADL        0
 17         CALL         eq      
 18         JUMPIF (0)   L12
 19         LOADL        0
 20         RETURN (1)   1
 21  L12:   LOAD         -1[LB]
 22         LOADL        1
 23         CALL         eq      
 24         JUMPIF (0)   L13
 25         LOADL        1
 26         RETURN (1)   1
 27  L13:   LOAD         -1[LB]
 28         LOADL        1
 29         CALL         sub     
 30         LOADA        0[OB]
 31         CALLI        L11
 32         LOAD         -1[LB]
 33         LOADL        2
 34         CALL         sub     
 35         LOADA        0[OB]
 36         CALLI        L11
 37         CALL         add     
 38         RETURN (1)   1
 39         RETURN (0)   1
