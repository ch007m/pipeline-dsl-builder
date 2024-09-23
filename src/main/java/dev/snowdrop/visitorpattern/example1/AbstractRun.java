package dev.snowdrop.visitorpattern.example1;

public abstract class AbstractRun implements Visitable {

    public AbstractRun() {}

    @Override
    public abstract AbstractRun accept(Visitor visitor);
}
