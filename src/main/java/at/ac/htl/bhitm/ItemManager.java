package at.ac.htl.bhitm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemManager {
    private HashMap<Integer, Item> items;

    public ItemManager() {
        items = new HashMap<>();
    }

    public void addItem(Item item) {
        items.put(item.getId(), item);
    }

    public void removeItem(Item item) {
        items.remove(item.getId());
    }

    public void removeItemById(int id) {
        items.remove(id);
    }

    public Item getItemById(int id) {
        if (!items.containsKey(id)) {
            return null;
        }
        return items.get(id);
    }

    public ArrayList<Item> getItems() {
        return new ArrayList<>(items.values());
    }

    public ArrayList<Item> getItemsByStatus(ItemLevel status) {
        ArrayList<Item> itemsByStatus = new ArrayList<>();
        for(Item item : items.values()) {
            if(item.getCurrentStatus() == status) {
                itemsByStatus.add(item);
            }
        }
        return itemsByStatus;
    }

    public void AddItemsToFile(String path) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("titel;description;currentStatus;imgPath;dateAdded");
            for(Item item : items.values()) {
                String currentStringToAdd = String.format("%s;%s;%s;%s;%s", item.getTitle(), item.getDescription(), item.getCurrentStatus(), item.getImgPath(), item.getDate());
                lines.add(currentStringToAdd);
            }
            Path filePath = Paths.get(path);
            Files.write(filePath, lines);
        } catch(IOException e) {
            throw new ItemException("Something happened while loading to file!", e);
        }
    }

    public void AddItemsFromFile(String path, ItemFactory factory) {
        try {
            boolean isHeader = true;
            List<String> lines = Files.readAllLines(Paths.get(path));

            for(String line : lines) {
                if(isHeader) {
                    isHeader = false;
                } else {
                    addItem(factory.createFromString(line));
                }
            }
        } catch(IOException e) {
            throw new ItemException("Something happened while loading from file!", e);
        }
    }
}