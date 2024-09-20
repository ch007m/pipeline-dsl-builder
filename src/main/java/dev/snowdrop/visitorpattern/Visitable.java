package dev.snowdrop.visitorpattern;

public interface Visitable {
    void accept(Visitor visitor);
}

