package dev.snowdrop.visitorpattern.example2;

interface Visitor {
    void visit(Book book);

    void visit(Vegetable vegetable);

    void visit(Fruit fruit);
}
