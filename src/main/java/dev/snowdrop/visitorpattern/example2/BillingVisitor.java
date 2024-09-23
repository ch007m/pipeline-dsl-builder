package dev.snowdrop.visitorpattern.example2;

public class BillingVisitor implements Visitor {
    double totalPrice = 0.0;

    @Override
    public void visit(Book book) {
        totalPrice += (book.getQuantity() * book.getPrice());
    }

    @Override
    public void visit(Vegetable vegetable) {
        totalPrice += (vegetable.getWeight() * vegetable.getPrice());
    }

    @Override
    public void visit(Fruit fruit) {
        totalPrice += (fruit.getQuantity() * fruit.getPrice());
    }

}
