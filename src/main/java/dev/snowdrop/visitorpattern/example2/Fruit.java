package dev.snowdrop.visitorpattern.example2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Fruit implements Visitable {
    private double quantity;
    private double price;

    public Fruit(double quantity, double price) {
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
