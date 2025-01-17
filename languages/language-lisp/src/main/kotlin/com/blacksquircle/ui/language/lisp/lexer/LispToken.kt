/*
 * Copyright 2022 Squircle CE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blacksquircle.ui.language.lisp.lexer

enum class LispToken {
    LONG_LITERAL,
    INTEGER_LITERAL,
    FLOAT_LITERAL,
    DOUBLE_LITERAL,

    DEFCLASS,
    DEFCONSTANT,
    DEFGENERIC,
    DEFINE_COMPILER_MACRO,
    DEFINE_CONDITION,
    DEFINE_METHOD_COMBINATION,
    DEFINE_MODIFY_MACRO,
    DEFINE_SETF_EXPANDER,
    DEFINE_SYMBOL_MACRO,
    DEFMACRO,
    DEFMETHOD,
    DEFPACKAGE,
    DEFPARAMETER,
    DEFSETF,
    DEFSTRUCT,
    DEFTYPE,
    DEFUN,
    DEFVAR,
    ABORT,
    ASSERT,
    BLOCK,
    BREAK,
    CASE,
    CATCH,
    CCASE,
    CERROR,
    COND,
    CTYPECASE,
    DECLAIM,
    DECLARE,
    DO,
    DO_S,
    DO_ALL_SYMBOLS,
    DO_EXTERNAL_SYMBOLS,
    DO_SYMBOLS,
    DOLIST,
    DOTIMES,
    ECASE,
    ERROR,
    ETYPECASE,
    EVAL_WHEN,
    FLET,
    HANDLER_BIND,
    HANDLER_CASE,
    IF,
    IGNORE_ERRORS,
    IN_PACKAGE,
    LABELS,
    LAMBDA,
    LET,
    LET_S,
    LOCALLY,
    LOOP,
    MACROLET,
    MULTIPLE_VALUE_BIND,
    PROCLAIM,
    PROG,
    PROG_S,
    PROG1,
    PROG2,
    PROGN,
    PROGV,
    PROVIDE,
    REQUIRE,
    RESTART_BIND,
    RESTART_CASE,
    RESTART_NAME,
    RETURN,
    RETURN_FROM,
    SIGNAL,
    SYMBOL_MACROLET,
    TAGBODY,
    THE,
    THROW,
    TYPECASE,
    UNLESS,
    UNWIND_PROTECT,
    WHEN,
    WITH_ACCESSORS,
    WITH_COMPILATION_UNIT,
    WITH_CONDITION_RESTARTS,
    WITH_HASH_TABLE_ITERATOR,
    WITH_INPUT_FROM_STRING,
    WITH_OPEN_FILE,
    WITH_OPEN_STREAM,
    WITH_OUTPUT_TO_STRING,
    WITH_PACKAGE_ITERATOR,
    WITH_SIMPLE_RESTART,
    WITH_SLOTS,
    WITH_STANDARD_IO_SYNTAX,

    TRUE,
    FALSE,
    NULL,

    EQEQ,
    NOTEQ,
    OROR,
    PLUSPLUS,
    MINUSMINUS,

    LT,
    LTLT,
    LTEQ,
    LTLTEQ,

    GT,
    GTGT,
    GTGTGT,
    GTEQ,
    GTGTEQ,
    GTGTGTEQ,

    AND,
    ANDAND,

    PLUSEQ,
    MINUSEQ,
    MULTEQ,
    DIVEQ,
    ANDEQ,
    OREQ,
    XOREQ,
    MODEQ,

    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    LBRACK,
    RBRACK,
    COMMA,
    DOT,

    EQ,
    NOT,
    TILDE,
    QUEST,
    COLON,
    PLUS,
    MINUS,
    MULT,
    DIV,
    OR,
    XOR,
    MOD,
    AT,
    BACKTICK,
    SINGLE_QUOTE,

    DOUBLE_QUOTED_STRING,

    LINE_COMMENT,
    BLOCK_COMMENT,

    IDENTIFIER,
    WHITESPACE,
    BAD_CHARACTER,
    EOF
}