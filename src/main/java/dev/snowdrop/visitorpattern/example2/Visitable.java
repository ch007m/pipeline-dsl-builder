package dev.snowdrop.visitorpattern.example2;

//Element
interface Visitable {
    public void accept(Visitor visitor);
}
