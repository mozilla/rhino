DEFAULT_OPTS = [
    "-XepAllErrorsAsWarnings",
]

LIBRARY_OPTS = DEFAULT_OPTS + [
    "-XepAllSuggestionsAsWarnings",
    "-Xlint:deprecation",
    "-Xlint:unchecked",
    "-Xlint:-removal",
    # Checks we aren't ready to fix yet
    "-Xep:ClassInitializationDeadlock:OFF",
    "-Xep:EffectivelyPrivate:OFF",
    "-Xep:BooleanLiteral:OFF",
    "-Xep:AssignmentExpression:OFF",
    "-Xep:DuplicateBranches:OFF",
    "-Xep:PatternMatchingInstanceof:OFF",
    "-Xep:RedundantControlFlow:OFF",
    "-Xep:StatementSwitchToExpressionSwitch:OFF",
    "-Xep:EmptyBlockTag:OFF",
    "-Xep:EscapedEntity:OFF",
    "-Xep:MissingSummary:OFF",
    "-Xep:InvalidBlockTag:OFF",
    "-Xep:UnicodeEscape:OFF",
    "-Xep:EmptyCatch:OFF",
    "-Xep:LabelledBreakTarget:OFF",
    "-Xep:JavaUtilDate:OFF",
    "-Xep:InlineMeSuggester:OFF",
    "-Xep:UnusedVariable:OFF",
    "-Xep:AnnotateFormatMethod:OFF",
    "-Xep:ImmutableEnumChecker:OFF",
    "-Xep:DoNotCallSuggester:OFF",
    # Additional useful checks
    "-Xep:RemoveUnusedImports:WARN",
    "-Xep:RemoveWildcardImport:WARN",
    "-Xep:UnusedMethod:WARN", 
]

TEST_OPTS = DEFAULT_OPTS + [
    "-Xlint:-deprecation",
    "-XepDisableAllChecks",
]
