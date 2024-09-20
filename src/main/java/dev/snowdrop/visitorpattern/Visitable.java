package dev.snowdrop.visitorpattern;

public interface Visitable {
    AbstractRun accept(Visitor visitor);
}

