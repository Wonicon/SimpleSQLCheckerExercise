enum Token {
  SELECT,  // select
  WHERE,   // where
  FROM,    // from
  COMMA,   // ,
  SEMI,    // ;
  STAR,    // *
  OP,      // binary operation, ignore precedence.
  PRED,    // >, <, =, ...
  BOOL,    // AND, OR
  LP,      // (
  RP,      // )
  ITEM,    // w+
  StringLiteral,
  POINT,
  NonToken,
  Term,
  END
}