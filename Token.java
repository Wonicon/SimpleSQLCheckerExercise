enum Token {
  SELECT,  // select
  WHERE,   // where
  FROM,    // from
  COMMA,   // ,
  SEMI,    // ;
  STAR,    // *
  OP,      // binary operation, ignore precedence.
  CMP,    // >, <, =, ...
  BOOL,    // AND, OR
  LP,      // (
  RP,      // )
  ITEM,    // w+
  StringLiteral,
  NonToken,
  END
}