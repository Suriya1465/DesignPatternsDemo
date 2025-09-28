// DesignPatternsDemo.java
public class DesignPatternsDemo {
    public static void main(String[] args) {
        System.out.println("===== Behavioral Patterns =====");
        observerPatternDemo();
        strategyPatternDemo();

        System.out.println("\n===== Creational Patterns =====");
        singletonPatternDemo();
        factoryPatternDemo();

        System.out.println("\n===== Structural Patterns =====");
        adapterPatternDemo();
        decoratorPatternDemo();
    }

    // -------------------- BEHAVIORAL PATTERNS --------------------

    // 1. Observer Pattern (Weather updates)
    static void observerPatternDemo() {
        System.out.println("-- Observer Pattern --");
        WeatherStation station = new WeatherStation();
        station.addObserver(new PhoneDisplay("Phone"));
        station.addObserver(new LaptopDisplay("Laptop"));
        station.setTemperature(30);
        station.setTemperature(35);
    }

    interface Observer {
        void update(int temp);
    }
    static class WeatherStation {
        private java.util.List<Observer> observers = new java.util.ArrayList<>();
        private int temperature;
        void addObserver(Observer o) { observers.add(o); }
        void setTemperature(int t) {
            this.temperature = t;
            notifyAllObservers();
        }
        void notifyAllObservers() {
            for (Observer o : observers) o.update(temperature);
        }
    }
    static class PhoneDisplay implements Observer {
        String name;
        PhoneDisplay(String n){name=n;}
        public void update(int temp){System.out.println(name+" shows temp: "+temp);}
    }
    static class LaptopDisplay implements Observer {
        String name;
        LaptopDisplay(String n){name=n;}
        public void update(int temp){System.out.println(name+" shows temp: "+temp);}
    }

    // 2. Strategy Pattern (Payment method selection)
    static void strategyPatternDemo() {
        System.out.println("-- Strategy Pattern --");
        PaymentContext context = new PaymentContext(new CreditCardPayment());
        context.pay(500);
        context = new PaymentContext(new UPIPayment());
        context.pay(250);
    }

    interface PaymentStrategy { void pay(int amount); }
    static class CreditCardPayment implements PaymentStrategy {
        public void pay(int amount){System.out.println("Paid "+amount+" using Credit Card");}
    }
    static class UPIPayment implements PaymentStrategy {
        public void pay(int amount){System.out.println("Paid "+amount+" using UPI");}
    }
    static class PaymentContext {
        private PaymentStrategy strategy;
        PaymentContext(PaymentStrategy s){ this.strategy = s; }
        void pay(int amt){ strategy.pay(amt); }
    }

    // -------------------- CREATIONAL PATTERNS --------------------

    // 3. Singleton Pattern (Logger)
    static void singletonPatternDemo() {
        System.out.println("-- Singleton Pattern --");
        Logger l1 = Logger.getInstance();
        Logger l2 = Logger.getInstance();
        l1.log("First log message");
        l2.log("Second log message");
        System.out.println("Both logger objects same? " + (l1 == l2));
    }
    static class Logger {
        private static Logger instance;
        private Logger() {}
        public static Logger getInstance() {
            if (instance == null) instance = new Logger();
            return instance;
        }
        public void log(String msg){ System.out.println("[LOG] "+msg); }
    }

    // 4. Factory Pattern (Shape creation)
    static void factoryPatternDemo() {
        System.out.println("-- Factory Pattern --");
        Shape circle = ShapeFactory.getShape("CIRCLE");
        Shape square = ShapeFactory.getShape("SQUARE");
        circle.draw();
        square.draw();
    }
    interface Shape { void draw(); }
    static class Circle implements Shape {
        public void draw(){ System.out.println("Drawing Circle"); }
    }
    static class Square implements Shape {
        public void draw(){ System.out.println("Drawing Square"); }
    }
    static class ShapeFactory {
        static Shape getShape(String type){
            if(type.equalsIgnoreCase("CIRCLE")) return new Circle();
            else if(type.equalsIgnoreCase("SQUARE")) return new Square();
            return null;
        }
    }

    // -------------------- STRUCTURAL PATTERNS --------------------

    // 5. Adapter Pattern (Charger adapter)
    static void adapterPatternDemo() {
        System.out.println("-- Adapter Pattern --");
        MobileCharger charger = new ChargerAdapter(new OldCharger());
        charger.chargePhone();
    }

    interface MobileCharger { void chargePhone(); }
    static class OldCharger {
        void supplyPower(){ System.out.println("Old charger supplying 110V power"); }
    }
    static class ChargerAdapter implements MobileCharger {
        private OldCharger oldCharger;
        ChargerAdapter(OldCharger oc){ this.oldCharger = oc; }
        public void chargePhone(){
            oldCharger.supplyPower();
            System.out.println("Adapter converts power to 5V for Phone charging");
        }
    }

    // 6. Decorator Pattern (Adding features to Coffee)
    static void decoratorPatternDemo() {
        System.out.println("-- Decorator Pattern --");
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " $" + coffee.cost());
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " $" + coffee.cost());
        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " $" + coffee.cost());
    }

    interface Coffee {
        String getDescription();
        double cost();
    }
    static class SimpleCoffee implements Coffee {
        public String getDescription(){ return "Simple Coffee"; }
        public double cost(){ return 5; }
    }
    static abstract class CoffeeDecorator implements Coffee {
        protected Coffee coffee;
        CoffeeDecorator(Coffee c){ this.coffee=c; }
    }
    static class MilkDecorator extends CoffeeDecorator {
        MilkDecorator(Coffee c){ super(c); }
        public String getDescription(){ return coffee.getDescription()+", Milk"; }
        public double cost(){ return coffee.cost()+2; }
    }
    static class SugarDecorator extends CoffeeDecorator {
        SugarDecorator(Coffee c){ super(c); }
        public String getDescription(){ return coffee.getDescription()+", Sugar"; }
        public double cost(){ return coffee.cost()+1; }
    }
}
