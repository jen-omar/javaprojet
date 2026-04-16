package tn.esprit.mythoria.entity;

public class Local {
    private int id;
    private String name;
    private String description;
    private double price;
    private String address;
    private int capacity;
    private String image;
    private String status;
    public Local() {
    }

    public Local(int id, String name, String description, double price, String address, int capacity, String image, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.address = address;
        this.capacity = capacity;
        this.image = image;
        this.status = status;
    }

    public Local(String name, String description, double price, String address, int capacity, String image, String status) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.address = address;
        this.capacity = capacity;
        this.image = image;
        this.status = status;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }



    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }



    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }



    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }



    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    @Override
    public String toString() {
        return name + " | " +
                description + " | " +
                price + " DT | " +
                address + " | " +
                capacity + " places | " +
                status;
    }

}
