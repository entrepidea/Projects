package com.entrepidea.core.design_pattern;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @Desc:
 * Visitor pattern involves two abstract/interface types, with referencing to each other. When the method of one type is invoked, the strategy is implemented inside the other.
 * JDK example include FileVistor and SimpleFileVisitor
 *
 * @Date: 04/16/20
 *
 * */
public class VisitorTests {
    /**
     * BNP Paribas on-site, 02/28/20
     * what's the visitor pattern?
     * */
    //Simply put, Visitor pattern separates algorithms from the object it works on. It tries as much as possible to avoid restructing the target class by
    //creating a separate visitor class to offer functionalities for the target class. On the other hand the targeting class must inherit a method which takes in
    // a visitor reference and implement it with customized business logic that only concerns the hosting class itself. This is called "double dispatching"
    //

    //Example: JDK's FileVisitor interface and SimpleFileVisitor class.
    //this link: https://howtodoinjava.com/java/io/delete-directory-recursively/ shows how to use JDK API - SimpleFileVisitor to remove folder/files recursively
    @Test
    public void testDeleteDirectory(){
        Path dir = Paths.get("C:\\Users\\jonat\\Downloads\\innerDir");
        try
        {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException
                {
                    System.out.println("Deleting file: " + file);
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException exc) throws IOException
                {
                    System.out.println("Deleting dir: " + dir);
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw exc;
                    }
                }

            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    //below example was shamelessly taken from https://refactoring.guru/design-patterns/visitor/java/example

    //target classes
    interface Shape {
        void move(int x, int y);
        void draw();
        String accept(Visitor visitor);
    }

    class Dot implements Shape {
        private int id;
        private int x;
        private int y;

        public Dot() {
        }

        public Dot(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        @Override
        public void move(int x, int y) {
            // move shape
        }

        @Override
        public void draw() {
            // draw shape
        }

        @Override
        public String accept(Visitor visitor) {
            return visitor.visitDot(this);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getId() {
            return id;
        }
    }

    class Circle extends Dot {
        private int radius;

        public Circle(int id, int x, int y, int radius) {
            super(id, x, y);
            this.radius = radius;
        }

        @Override
        public String accept(Visitor visitor) {
            return visitor.visitCircle(this);
        }

        public int getRadius() {
            return radius;
        }
    }

    class Rectangle implements Shape {
        private int id;
        private int x;
        private int y;
        private int width;
        private int height;

        public Rectangle(int id, int x, int y, int width, int height) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String accept(Visitor visitor) {
            return visitor.visitRectangle(this);
        }

        @Override
        public void move(int x, int y) {
            // move shape
        }

        @Override
        public void draw() {
            // draw shape
        }

        public int getId() {
            return id;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    class CompoundShape implements Shape {
        public int id;
        public List<Shape> children = new ArrayList<>();

        public CompoundShape(int id) {
            this.id = id;
        }

        @Override
        public void move(int x, int y) {
            // move shape
        }

        @Override
        public void draw() {
            // draw shape
        }

        public int getId() {
            return id;
        }

        @Override
        public String accept(Visitor visitor) {
            return visitor.visitCompoundGraphic(this);
        }

        public void add(Shape shape) {
            children.add(shape);
        }
    }

    //visitor interface
    interface Visitor {
        String visitDot(Dot dot);

        String visitCircle(Circle circle);

        String visitRectangle(Rectangle rectangle);

        String visitCompoundGraphic(CompoundShape cg);
    }

    class XMLExportVisitor implements Visitor {

        public String export(Shape... args) {
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n");
            for (Shape shape : args) {
                sb.append(shape.accept(this)).append("\n");
            }
            return sb.toString();
        }

        public String visitDot(Dot d) {
            return "<dot>" + "\n" +
                    "    <id>" + d.getId() + "</id>" + "\n" +
                    "    <x>" + d.getX() + "</x>" + "\n" +
                    "    <y>" + d.getY() + "</y>" + "\n" +
                    "</dot>";
        }

        public String visitCircle(Circle c) {
            return "<circle>" + "\n" +
                    "    <id>" + c.getId() + "</id>" + "\n" +
                    "    <x>" + c.getX() + "</x>" + "\n" +
                    "    <y>" + c.getY() + "</y>" + "\n" +
                    "    <radius>" + c.getRadius() + "</radius>" + "\n" +
                    "</circle>";
        }

        public String visitRectangle(Rectangle r) {
            return "<rectangle>" + "\n" +
                    "    <id>" + r.getId() + "</id>" + "\n" +
                    "    <x>" + r.getX() + "</x>" + "\n" +
                    "    <y>" + r.getY() + "</y>" + "\n" +
                    "    <width>" + r.getWidth() + "</width>" + "\n" +
                    "    <height>" + r.getHeight() + "</height>" + "\n" +
                    "</rectangle>";
        }

        public String visitCompoundGraphic(CompoundShape cg) {
            return "<compound_graphic>" + "\n" +
                    "   <id>" + cg.getId() + "</id>" + "\n" +
                    _visitCompoundGraphic(cg) +
                    "</compound_graphic>";
        }

        private String _visitCompoundGraphic(CompoundShape cg) {
            StringBuilder sb = new StringBuilder();
            for (Shape shape : cg.children) {
                String obj = shape.accept(this);
                // Proper indentation for sub-objects.
                obj = "    " + obj.replace("\n", "\n    ") + "\n";
                sb.append(obj);
            }
            return sb.toString();
        }

    }

    private void export(Shape... shapes) {
        XMLExportVisitor exportVisitor = new XMLExportVisitor();
        System.out.println(exportVisitor.export(shapes));
    }
    @Test
    public void testXmlExportVisitor(){
        Dot dot = new Dot(1, 10, 55);
        Circle circle = new Circle(2, 23, 15, 10);
        Rectangle rectangle = new Rectangle(3, 10, 17, 20, 30);

        CompoundShape compoundShape = new CompoundShape(4);
        compoundShape.add(dot);
        compoundShape.add(circle);
        compoundShape.add(rectangle);

        CompoundShape c = new CompoundShape(5);
        c.add(dot);
        compoundShape.add(c);

        export(circle, compoundShape);
    }
}
