package dev.snowdrop.visitorpattern.example2;

import java.util.ArrayList;
import java.util.List;


public class MainApp {

    public static void main(String[] args) {
        List<Visitable> visitableElements = new ArrayList<Visitable>();
        visitableElements.add(new Book("I123",10,2.0));
        visitableElements.add(new Fruit(5,7.0));
        visitableElements.add(new Vegetable(25,8.0));

        BillingVisitor billingVisitor = new BillingVisitor();
        for(Visitable visitableElement : visitableElements){
            visitableElement.accept(billingVisitor);
        }

        OfferVisitor offerVisitor = new OfferVisitor();
        for(Visitable visitableElement : visitableElements){
            visitableElement.accept(offerVisitor);
        }

        System.out.println("Total bill :" + billingVisitor.totalPrice);
        System.out.println("Offer  :" + offerVisitor.offer);

    }

}
