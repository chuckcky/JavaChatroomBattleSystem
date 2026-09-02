package model;

public abstract class Card {
    private String name;
    private int cost;


    public Card(String name, int cost) {
        this.name = name;
        this.cost = cost;
    }

    public abstract void execute(Player caster, Player target);


    /**
     * 获取
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * 设置
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取
     * @return cost
     */
    public int getCost() {
        return cost;
    }

    /**
     * 设置
     * @param cost
     */
    public void setCost(int cost) {
        this.cost = cost;
    }

    public String toString() {
        return name + "(费" + cost + ")";
    }
}
