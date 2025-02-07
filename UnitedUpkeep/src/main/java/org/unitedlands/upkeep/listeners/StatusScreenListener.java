package org.unitedlands.upkeep.listeners;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.TextComponent;
import com.palmergames.adventure.text.event.HoverEvent;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.format.TextDecoration;
import com.palmergames.adventure.text.minimessage.MiniMessage;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.upkeep.UnitedUpkeep;
import org.unitedlands.upkeep.calculators.TownUpkeepCalculator;

import static org.unitedlands.upkeep.util.NationMetaController.isOfficialNation;

public class StatusScreenListener implements Listener {
    private final UnitedUpkeep unitedUpkeep;
    private final @NotNull MiniMessage miniMessage = MiniMessage.miniMessage();
    private Town town;
    private StatusScreen screen;

    public StatusScreenListener(UnitedUpkeep unitedUpkeep) {
        this.unitedUpkeep = unitedUpkeep;
    }

    @EventHandler
    public void onStatusScreen(NationStatusScreenEvent event) {
        screen = event.getStatusScreen();
        if (isOfficialNation(event.getNation())) {
            addOfficialNationComponent();
        }
    }
    @EventHandler
    public void onStatusScreen(TownStatusScreenEvent event) {
        screen = event.getStatusScreen();
        town = event.getTown();
        replaceTownSizeComponent();
        replaceUpkeepComponent();
        if (town.hasNation()) {
            if (isOfficialNation(town.getNationOrNull()))
                addOfficialTownComponent();
        }
        // Remove the default neutrality cost.
        screen.removeStatusComponent("neutralityCost");
    }

    private void addOfficialTownComponent() {
        Component subtitle = screen.getComponentOrNull("subtitle");
        if (subtitle == null) {
            subtitle = screen.getComponentOrNull("title");
            screen.replaceComponent("title", subtitle.append(Component.newline().append(getOfficialTownComponent())));
            return;
        }
        screen.replaceComponent("subtitle", subtitle.append(Component.newline().append(getOfficialTownComponent())));
    }

    private void addOfficialNationComponent() {
        Component subtitle = screen.getComponentOrNull("subtitle");
        if (subtitle == null) {
            subtitle = screen.getComponentOrNull("nation_title");
            screen.replaceComponent("nation_title", subtitle.append(Component.newline().append(getOfficialNationComponent())));
            return;
        }
        screen.replaceComponent("subtitle", subtitle.append(Component.newline().append(getOfficialNationComponent())));
    }

    private TownUpkeepCalculator getTownUpkeepCalculator() {
        return new TownUpkeepCalculator(unitedUpkeep, town);
    }

    private int getTownsize() {
        return town.getTownBlocks().size();
    }

    private void replaceUpkeepComponent() {
        TextComponent upkeepComponent = getUpkeepComponent();

        if (getBonusBlockDiscount() > 0 && getNationDiscount() > 0) {
            upkeepComponent = upkeepComponent.hoverEvent(HoverEvent.showText(getComponentWithAllDiscounts()));
        } else if (getNationDiscount() > 0) {
            upkeepComponent = upkeepComponent.hoverEvent(HoverEvent.showText(getNationDiscountComponent().append(getNeutralityComponent())));
        } else if (getBonusBlockDiscount() > 0) {
            upkeepComponent = upkeepComponent.hoverEvent(HoverEvent.showText(getBonusDiscountComponent().append(getNeutralityComponent())));
        }

        screen.replaceComponent("upkeep", upkeepComponent);
    }

    private void replaceTownSizeComponent() {
        TextComponent townSizeComponent = Component
                .text("\nTown Size: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(getTownsize(), NamedTextColor.GREEN));
        screen.replaceComponent("townblocks", townSizeComponent);
    }

    private TextComponent getOfficialNationComponent() {
        return (TextComponent) miniMessage.deserialize("                          <gradient:#D4AF37:#FCFDD3><bold>OFFICIAL NATION</gradient>");
    }
    private TextComponent getOfficialTownComponent() {
        return (TextComponent) miniMessage.deserialize("                  <gradient:#D4AF37:#FCFDD3><bold>OFFICIAL NATION MEMBER</gradient>");
    }
    private TextComponent getComponentWithAllDiscounts() {
        return Component.text("")
                .append(getNationDiscountComponent())
                .append(Component.text("\n"))
                .append(getBonusDiscountComponent())
                .append(getNeutralityComponent());
    }

    private TextComponent getNationDiscountComponent() {
        return Component
                .text("[Nation Discount: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(getNationDiscount() + " Gold", NamedTextColor.GREEN))
                .append(Component.text("]", NamedTextColor.DARK_GREEN));
    }

    private TextComponent getBonusDiscountComponent() {
        return Component
                .text("[Bonus Discount: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(getBonusBlockDiscount() + " Gold (" + town.getBonusBlocks() + ")", NamedTextColor.GREEN))
                .append(Component.text("]", NamedTextColor.DARK_GREEN));
    }

    private TextComponent getNeutralityComponent() {
        // Return an empty component if the town isn't neutral.
        if (!town.isNeutral()) {
            return Component.empty();
        }
        double fee = getNeutralityFee();
        return Component
                .text("\n[Neutrality Fees: ", NamedTextColor.DARK_GREEN)
                .append(Component.text(fee + " Gold", NamedTextColor.RED))
                .append(Component.text("]", NamedTextColor.DARK_GREEN));
    }

    private double getNeutralityFee() {
        int defaultFee = 25;
        return Math.floor((getTownUpkeepCalculator().getDiscountedUpkeep() * 0.1) + defaultFee);
    }

    private TextComponent getUpkeepComponent() {
        var calculator = getTownUpkeepCalculator();
        double neutralityFee = 0.0;
        if (town.isNeutral()) {
            neutralityFee = getNeutralityFee();
        }
        double upkeep = calculator.calculateTownUpkeep() + neutralityFee;
        double discountedUpkeep = calculator.getDiscountedUpkeep() + neutralityFee;
        return Component
                .text("")
                .append(Component.text("\nUpkeep: ", NamedTextColor.DARK_GREEN))
                .append(Component.text(upkeep, NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                .append(Component.text( " " + discountedUpkeep + " Gold", NamedTextColor.RED));
    }

    private double getNationDiscount() {
        var calculator = getTownUpkeepCalculator();
        return calculator.getNationDiscount();
    }

    private double getBonusBlockDiscount() {
        var calculator = getTownUpkeepCalculator();
        return calculator.calculateBonusBlockDiscount();
    }
}
