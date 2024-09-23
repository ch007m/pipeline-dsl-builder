package dev.snowdrop.visitorpattern.example2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vegetable implements Visitable {
    private double weight;
    private double price;

    public Vegetable(double weight, double price) {
        this.weight = weight;
        this.price = price;
    }


    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
