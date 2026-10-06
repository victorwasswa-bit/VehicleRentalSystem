package org.example;

import com.toedter.calendar.JDateChooser;

import javax.swing.*;

public class TestDate {
    public static void main(String[] args) {
        JFrame f = new JFrame("Date test");
        f.add(new JDateChooser());
        f.setSize(300, 100);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}