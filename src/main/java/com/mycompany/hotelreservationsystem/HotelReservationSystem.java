/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.hotelreservationsystem;

import javax.swing.SwingUtilities;

public class HotelReservationSystem {

    public static void printWelcomeHomeScreen() {
        System.out.println("Welcome to Hotel Reservation Home Screen!");
    }

    public static void printOurFinalProject() {
        System.out.println("Our final project");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WelcomeScreen welcomeScreen = new WelcomeScreen();
            welcomeScreen.setVisible(true);
        });
     
    }
}
