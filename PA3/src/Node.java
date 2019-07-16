import java.util.Enumeration;
import java.util.Vector;

/**
 * A class representing a routing-capable node in the network.
 */
public class Node extends NodeBase {
    /**
     * Create a new node with the given address.
     */
    public Node(int address) {
        super(address);
    }

    /**
     * Broadcasting the routing table to the neighbours.
     */
    private void broadcast() {
        for (Object neighbours : interfaces) {
            Link link = (Link) neighbours;
            int linkOtherDest = link.getDest(getAddress());

            if (link.isUp()) {
                StringBuilder broadcastPackage = new StringBuilder();
                Enumeration routingTable = this.routingTable.enumerate();
                while (routingTable.hasMoreElements()) {
                    Route route = (Route) routingTable.nextElement();
                    // If the next hop is already the links destination there is no need to broadcast
                    if (route.getNextHop() != linkOtherDest) {
                        broadcastPackage.append(route.getDest()).append("->").append(route.getCost());
                        if (routingTable.hasMoreElements()) {
                            broadcastPackage.append("#");
                        }
                    }
                }
                sendPacket(linkOtherDest, broadcastPackage.toString());
            }
        }
    }

    private boolean removeFromTable(int source, String[] tokens) {
        Vector<Integer> lostsVector = new Vector<>();
        Vector<Integer> destinations = new Vector<>();
        for (String token : tokens) {
            destinations.add(Integer.valueOf(token.substring(0, token.indexOf("-"))));
        }
        Enumeration routingTable = this.routingTable.enumerate();
        while (routingTable.hasMoreElements()) {
            Route route = (Route) routingTable.nextElement();
            if (route.getNextHop() == source && !destinations.contains(route.getDest())) {
                lostsVector.add(route.getDest());
            }
        }
        Enumeration<Integer> lostsEnumeration = lostsVector.elements();
        while (lostsEnumeration.hasMoreElements()) {
            this.routingTable.remove(lostsEnumeration.nextElement());
        }
        return lostsVector.size() != 0;
    }

    private boolean updateTable(int source, String[] tokens) {
        boolean res = false;
        for (String token : tokens) {
            int tokenDest = Integer.valueOf(token.substring(0, token.indexOf("-")));
            int tokenCost = Integer.valueOf(token.substring(token.indexOf('>') + 1));
            Route tokenDestRoute = routingTable.findRoute(tokenDest);
            Route sourceRoute = routingTable.findRoute(source);
            if (tokenDestRoute == null || tokenDestRoute.getCost() > tokenCost + sourceRoute.getCost()) {
                if (tokenDestRoute != null) {
                    routingTable.remove(tokenDest);
                }
                routingTable.add(new Route(tokenDest, sourceRoute.getNextHop(), tokenCost + sourceRoute.getCost()));
                res = true;
            }
        }
        return res;
    }


    /**
     * Called when the node "boots".  This function should initialize
     * the node's routing table and send out appropriate boot-time packets.
     */
    public void init() {
        Route routeToSelf = new Route(getAddress(), getAddress(), 0);
        routingTable.add(routeToSelf);
        for (Object neighbours : interfaces) {
            Link link = (Link) neighbours;
            routingTable.add(new Route(link.getDest(getAddress()), link.getDest(getAddress()), link.getCost()));
        }
        broadcast();
        System.out.println("node " + getAddress() + " - initialized");
    }

    /**
     * Called when one of the node's interfaces is brought up.
     *
     * @param lnk The address of the node at the other end of the
     *            interface being brought up.
     */
    public void interfaceUp(Link lnk) {
        int linkOtherDest = lnk.getDest(getAddress());
        Route route = routingTable.findRoute(linkOtherDest);
        // We Already have a better route (smaller cost)
        if (route != null && route.getCost() < lnk.getCost()) {
            return;
        }
        routingTable.remove(linkOtherDest);
        routingTable.add(new Route(linkOtherDest, linkOtherDest, lnk.getCost()));
        broadcast();

        System.out.println("node " + getAddress() + " - interface to " +
                linkOtherDest + " up");
    }

    /**
     * Called when one of the node's interfaces is brought down.
     *
     * @param lnk The address of the node at the other end of the
     *            interface being brought down.
     */
    public void interfaceDown(Link lnk) {
        int linkOtherDest = lnk.getDest(getAddress());
        Route route = routingTable.findRouteByNextHop(linkOtherDest);
        // Removes all the routes which used linkOtherDest as the next hop
        while (route != null) {
            routingTable.remove(route.getDest());
            route = routingTable.findRouteByNextHop(linkOtherDest);
        }
        broadcast();

        System.out.println("node " + getAddress() + " - interface to " +
                linkOtherDest + " down");
    }

    /**
     * Called when the node receives a packet.
     *
     * @param source The node that sent the packet.
     * @param pkt    The packet itself.
     */
    public void receivePacket(int source, String pkt) {
        String[] tokens = pkt.split("#");
        if (removeFromTable(source, tokens) || updateTable(source, tokens)) {
            broadcast();
        }
        System.out.println("node " + getAddress() + " - received from " + source +
                ": \"" + pkt + "\"");
    }
}
