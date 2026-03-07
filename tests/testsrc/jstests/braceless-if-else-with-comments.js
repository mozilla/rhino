// Single comment between braceless if and its body
if (a)
    // single comment
    doA();
else
    doB();

// Two consecutive comments between braceless if and its body
if (x)
    // comment 1
    // comment 2
    doSomething();
else
    doSomethingElse();

// Three comments
if (y)
    // first
    // second
    // third
    doY();
else
    doZ();

// Comment in else branch
if (p)
    doP();
else
    // else comment
    doQ();

// Comments in both branches
if (m)
    // if comment
    doM();
else
    // else comment
    doN();

// Nested if/else with comments
if (a)
    // outer if comment
    if (b)
        // inner if comment
        // second inner comment
        doAB();
    else
        doA();
else
    doOther();
