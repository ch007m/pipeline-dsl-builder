package dev.snowdrop.visitorpattern.example2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Book implements Visitable {
    private String isbn;
    private double quantity;
    private double price;

    public Book(String isbn, double quantity, double price) {
        this.isbn = isbn;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
