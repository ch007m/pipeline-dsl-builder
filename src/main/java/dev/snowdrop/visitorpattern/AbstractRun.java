package dev.snowdrop.visitorpattern;

public abstract class AbstractRun implements Visitable {

    public AbstractRun() {}

    @Override
    public abstract AbstractRun accept(Visitor visitor);
}
