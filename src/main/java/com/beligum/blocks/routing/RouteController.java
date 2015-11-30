package com.beligum.blocks.routing;

/**
 * Created by wouter on 28/05/15.
 */
public class RouteController
{

    public static RouteController instance;

    private RouteController()
    {

    }

    public static RouteController instance()
    {
        if (RouteController.instance == null) {
            RouteController.instance = new RouteController();
        }
        return RouteController.instance;
    }

}
