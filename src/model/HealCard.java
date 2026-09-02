package model;

public class HealCard extends Card {
    private int healAmount;

    public HealCard(String name, int cost, int healAmount) {
        super(name, cost);
        this.healAmount = healAmount;
    }


    @Override
    public void execute(Player caster, Player target) {
        System.out.println(caster.getName() + "打出" + getName() + "，恢复 " + healAmount + " 点生命！");
        caster.heal(healAmount);
    }
}
