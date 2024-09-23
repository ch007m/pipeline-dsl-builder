package dev.snowdrop.visitorpattern.example1;

public interface Visitable {
    AbstractRun accept(Visitor visitor);
}

