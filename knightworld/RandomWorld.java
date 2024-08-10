package knightworld;

import byow.InputDemo.InputSource;
import byow.InputDemo.KeyboardInputSource;
import byow.InputDemo.RandomInputSource;
import byow.InputDemo.StringInputDevice;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;


import java.util.*;
import java.util.PriorityQueue;

public class RandomWorld {

    private TETile[][] tiles;
    private int width;
    private int height;
    private Random random;
    private List<Room> rooms;

    private boolean isLight = false;

    private boolean isLight1 = false;
    private boolean isLight2 = false;
    private boolean isLight3 = false;


    private int avatarX;
    private int avatarY;
    private static final int KEYBOARD = 0;

    private List<int[]> lightCoordinates = new ArrayList<>();  // 用于存储光源坐标


    public RandomWorld(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.random = new Random(seed);
        tiles = new TETile[width][height];
        rooms = new ArrayList<>();
        initializeTiles();
        generateRoomsAndPaths();
        fillblank();
        addLightsource();
        turnon();
        addAvatar();
        addCoins();

    }

    /** Initializes the world with walls. */
    private void initializeTiles() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = Tileset.WALL;
            }
        }
    }

    /** Generates random rooms and paths. */
    private void generateRoomsAndPaths() {
        int roomCount = random.nextInt(10) + 10; // Random number of rooms
        for (int i = 0; i < roomCount; i++) {
            int roomWidth = random.nextInt(8) + 3; // Random room width
            int roomHeight = random.nextInt(8) + 3; // Random room height
            int roomX = random.nextInt(width - roomWidth - 2) + 1;
            int roomY = random.nextInt(height - roomHeight - 2) + 1;
            addRoom(roomX, roomY, roomWidth, roomHeight);
        }
        connectRooms();
    }

    /** Adds a room to the world. */
    private void addRoom(int x, int y, int roomWidth, int roomHeight) {
        for (int i = x; i < x + roomWidth; i++) {
            for (int j = y; j < y + roomHeight; j++) {
                tiles[i][j] = Tileset.FLOOR;
            }
        }
        rooms.add(new Room(x, y, roomWidth, roomHeight));
    }

    /** Connects rooms with paths using MST. */
    private void connectRooms() {
        List<Edge> edges = new ArrayList<>();
        Map<Integer, Set<Integer>> connectedComponents = new HashMap<>();

        // Identify connected components
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                Room r1 = rooms.get(i);
                Room r2 = rooms.get(j);
                if (areRoomsConnected(r1, r2)) {
                    connectedComponents.computeIfAbsent(i, k -> new HashSet<>()).add(j);
                    connectedComponents.computeIfAbsent(j, k -> new HashSet<>()).add(i);
                }
            }
        }

        // Calculate distances between all pairs of rooms that are not already connected
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                if (!isConnected(connectedComponents, i, j)) {
                    Room r1 = rooms.get(i);
                    Room r2 = rooms.get(j);
                    double distance = Math.sqrt(Math.pow(r1.centerX() - r2.centerX(), 2) + Math.pow(r1.centerY() - r2.centerY(), 2));
                    edges.add(new Edge(i, j, distance));
                }
            }
        }

        // Sort edges by distance
        edges.sort(Comparator.comparingDouble(e -> e.distance));
        // Kruskal's algorithm to find MST
        int[] parent = new int[rooms.size()];
        initializeParent(parent, connectedComponents);
        System.out.println("Parent array after initialization: " + Arrays.toString(parent));

        List<Edge> mst = new ArrayList<>();
        for (Edge edge : edges) {
            int root1 = find(parent, edge.room1);
            int root2 = find(parent, edge.room2);
            if (root1 != root2) {
                mst.add(edge);
                union(parent, root1, root2);
                connectComponents(connectedComponents, edge.room1, edge.room2);
            }
        }

        // Generate paths based on MST
        for (Edge edge : mst) {
            Room r1 = rooms.get(edge.room1);
            Room r2 = rooms.get(edge.room2);
            generatePath(r1.centerX(), r1.centerY(), r2.centerX(), r2.centerY());
        }
    }

    private void fillblank() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // 如果当前位置是墙壁，判断周围的八个位置是否有地板，如果没有，将当前位置设置为nothing
                if (tiles[x][y] == Tileset.WALL) {
                    if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                        if (tiles[x - 1][y] != Tileset.FLOOR && tiles[x + 1][y] != Tileset.FLOOR &&
                                tiles[x][y - 1] != Tileset.FLOOR && tiles[x][y + 1] != Tileset.FLOOR &&
                                tiles[x - 1][y - 1] != Tileset.FLOOR && tiles[x + 1][y - 1] != Tileset.FLOOR &&
                                tiles[x - 1][y + 1] != Tileset.FLOOR && tiles[x + 1][y + 1] != Tileset.FLOOR) {
                            tiles[x][y] = Tileset.NOTHING;
                        }
                    }
                }
            }
        }
        //现在还有上下左右最边缘的边没有判断，所以再判断一次，但是不能超出index范围
        for (int x = 0; x < width; x++) {
            if (tiles[x][0] == Tileset.WALL) {
                if (x > 0 && x < width - 1) {
                    if (tiles[x - 1][0] != Tileset.FLOOR && tiles[x + 1][0] != Tileset.FLOOR &&
                            tiles[x][1] != Tileset.FLOOR && tiles[x - 1][1] != Tileset.FLOOR &&
                            tiles[x + 1][1] != Tileset.FLOOR) {
                        tiles[x][0] = Tileset.NOTHING;
                    }
                }
            }
            if (tiles[x][height - 1] == Tileset.WALL) {
                if (x > 0 && x < width - 1) {
                    if (tiles[x - 1][height - 1] != Tileset.FLOOR && tiles[x + 1][height - 1] != Tileset.FLOOR &&
                            tiles[x][height - 2] != Tileset.FLOOR && tiles[x - 1][height - 2] != Tileset.FLOOR &&
                            tiles[x + 1][height - 2] != Tileset.FLOOR) {
                        tiles[x][height - 1] = Tileset.NOTHING;
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            if (tiles[0][y] == Tileset.WALL) {
                if (y > 0 && y < height - 1) {
                    if (tiles[0][y - 1] != Tileset.FLOOR && tiles[0][y + 1] != Tileset.FLOOR &&
                            tiles[1][y] != Tileset.FLOOR && tiles[1][y - 1] != Tileset.FLOOR &&
                            tiles[1][y + 1] != Tileset.FLOOR) {
                        tiles[0][y] = Tileset.NOTHING;
                    }
                }
            }
            if (tiles[width - 1][y] == Tileset.WALL) {
                if (y > 0 && y < height - 1) {
                    if (tiles[width - 1][y - 1] != Tileset.FLOOR && tiles[width - 1][y + 1] != Tileset.FLOOR &&
                            tiles[width - 2][y] != Tileset.FLOOR && tiles[width - 2][y - 1] != Tileset.FLOOR &&
                            tiles[width - 2][y + 1] != Tileset.FLOOR) {
                        tiles[width - 1][y] = Tileset.NOTHING;
                    }
                }
            }
        }
        //最后再判断四个角
        if (tiles[0][0] == Tileset.WALL) {
            if (tiles[0][1] != Tileset.FLOOR && tiles[1][0] != Tileset.FLOOR && tiles[1][1] != Tileset.FLOOR) {
                tiles[0][0] = Tileset.NOTHING;
            }
        }
        if (tiles[width - 1][0] == Tileset.WALL) {
            if (tiles[width - 1][1] != Tileset.FLOOR && tiles[width - 2][0] != Tileset.FLOOR && tiles[width - 2][1] != Tileset.FLOOR) {
                tiles[width - 1][0] = Tileset.NOTHING;
            }
        }
        if (tiles[0][height - 1] == Tileset.WALL) {
            if (tiles[0][height - 2] != Tileset.FLOOR && tiles[1][height - 1] != Tileset.FLOOR && tiles[1][height - 2] != Tileset.FLOOR) {
                tiles[0][height - 1] = Tileset.NOTHING;
            }
        }
        if (tiles[width - 1][height - 1] == Tileset.WALL) {
            if (tiles[width - 1][height - 2] != Tileset.FLOOR && tiles[width - 2][height - 1] != Tileset.FLOOR && tiles[width - 2][height - 2] != Tileset.FLOOR) {
                tiles[width - 1][height - 1] = Tileset.NOTHING;
            }
        }
    }

    private void addAvatar() {
        int roomIndex = random.nextInt(rooms.size());
        Room room = rooms.get(roomIndex);
        avatarX = room.centerX();
        avatarY = room.centerY();
        tiles[avatarX][avatarY] = Tileset.AVATAR;
    }

    private void moveAvatar(char c) {
        int newX = avatarX;
        int newY = avatarY;
        switch (c) {
            case 'W':
            case 'w':
                newY++;
                break;
            case 'A':
            case 'a':
                newX--;
                break;
            case 'S':
            case 's':
                newY--;
                break;
            case 'D':
            case 'd':
                newX++;
                break;
            default:
                return;
        }

        // 检查新位置是否不是墙
        if (tiles[newX][newY] != Tileset.WALL) {
            // 如果 avatar 目前位于光源位置，将当前位置设置为光源
            if (isLight) {
                tiles[avatarX][avatarY] = Tileset.LIGHTSOURCE;
                isLight = false;
            } else if (isLight1) {
                tiles[avatarX][avatarY] = Tileset.LIGHT1;
                isLight1 = false;
            } else if (isLight2) {
                tiles[avatarX][avatarY] = Tileset.LIGHT2;
                isLight2 = false;
            } else if (isLight3) {
                tiles[avatarX][avatarY] = Tileset.LIGHT3;
                isLight3 = false;
            } else {
                tiles[avatarX][avatarY] = Tileset.FLOOR;
            }

            // 更新 avatar 位置
            avatarX = newX;
            avatarY = newY;

            // 检查新位置是否是光源
            if (tiles[newX][newY] == Tileset.LIGHTSOURCE) {
                isLight = true;
            }else if (tiles[newX][newY] == Tileset.LIGHT1) {
                isLight1 = true;
            }else if (tiles[newX][newY] == Tileset.LIGHT2) {
                isLight2 = true;
            }else if (tiles[newX][newY] == Tileset.LIGHT3) {
                isLight3 = true;
            }

            // 设置新位置为 avatar
            tiles[avatarX][avatarY] = Tileset.AVATAR;
        }
    }

    private void addCoins(){
        int i = 0;
        int coinCount = random.nextInt(3) +2;
        while (i < coinCount){
            int coinX = random.nextInt(width -2) + 1;
            int coinY = random.nextInt(height -2) + 1;
            if (tiles[coinX][coinY] == Tileset.FLOOR){
                tiles[coinX][coinY] = Tileset.COIN;
                i++;
            }

        }
    }

    private void eatCoin(){
        if (tiles[avatarX][avatarY] == Tileset.COIN){
            tiles[avatarX][avatarY] = Tileset.FLOOR;
        }
    }

    // return the coordinate of the lightsource

    private void addLightsource() {
        int lightcount = random.nextInt(3) + 3;
        ArrayList<Integer> roomidx = new ArrayList<>();

        for (int i = 0; i < lightcount; i++) {
            int roomIndex = random.nextInt(rooms.size());
            if (!roomidx.contains(roomIndex)) {
                roomidx.add(roomIndex);
                Room room = rooms.get(roomIndex);
                int lightLocationX = random.nextInt(room.width) - room.width / 2;
                int lightLocationY = random.nextInt(room.height) - room.height / 2;
                int lightX = room.centerX() + lightLocationX;
                int lightY = room.centerY() + lightLocationY;
                tiles[lightX][lightY] = Tileset.LIGHTSOURCE;
                lightCoordinates.add(new int[]{lightX, lightY});  // 将光源坐标添加到列表中
            }
        }
    }

    private void turnonLight(int[] lightcoordinate) {
        int Xcoord = lightcoordinate[0];
        int Ycoord = lightcoordinate[1];

        // 定义光源半径和对应的光源强度
        int[] radii = {3, 2, 1};
        TETile[] lightLevels = {Tileset.LIGHT3, Tileset.LIGHT2, Tileset.LIGHT1};

        for (int i = 0; i < radii.length; i++) {
            int radius = radii[i];
            TETile lightLevel = lightLevels[i];
            illuminateArea(Xcoord, Ycoord, radius, lightLevel);
        }
    }

    // 辅助方法，用于设置特定半径范围内的光源强度
    private void illuminateArea(int Xcoord, int Ycoord, int radius, TETile lightLevel) {
        for (int x = Xcoord - radius; x <= Xcoord + radius; x++) {
            for (int y = Ycoord - radius; y <= Ycoord + radius; y++) {
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    if (tiles[x][y] == Tileset.FLOOR || tiles[x][y] == Tileset.LIGHT3 || tiles[x][y] == Tileset.LIGHT2){
                        tiles[x][y] = lightLevel;
                    }
                }
            }
        }
    }



    private void turnon(){
        for (int[] lightcoordinate : lightCoordinates){
            turnonLight(lightcoordinate);
        }
    }

    private int find(int[] parent, int x) {
        if (parent[x] != x) {
            parent[x] = find(parent, parent[x]);
        }
        return parent[x];
    }

    private boolean areRoomsConnected(Room r1, Room r2) {
        // Check if rooms are adjacent or overlap
        return (r1.x < r2.x + r2.width && r1.x + r1.width > r2.x &&
                r1.y < r2.y + r2.height && r1.y + r1.height > r2.y);
    }

    private boolean isConnected(Map<Integer, Set<Integer>> connectedComponents, int room1, int room2) {
        return connectedComponents.containsKey(room1) && connectedComponents.get(room1).contains(room2);
    }

    private void connectComponents(Map<Integer, Set<Integer>> connectedComponents, int room1, int room2) {
        connectedComponents.computeIfAbsent(room1, k -> new HashSet<>()).add(room2);
        connectedComponents.computeIfAbsent(room2, k -> new HashSet<>()).add(room1);
    }

    private void union(int[] parent, int root1, int root2) {
        int newRoot = find(parent, root2); // Ensure we use the root of root2 as the new root
        for (int i = 0; i < parent.length; i++) {
            if (find(parent, i) == root1) {
                parent[i] = newRoot;
            }
        }
    }

    private void initializeParent(int[] parent, Map<Integer, Set<Integer>> connectedComponents) {
        // 初始化每个房间的父节点为自身
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }

        // 遍历 connectedComponents 中的每个条目
        for (Map.Entry<Integer, Set<Integer>> entry : connectedComponents.entrySet()) {
            // 获取当前条目的键值对：房间索引和连接分量集合
            int root = find(parent, entry.getKey());  // 找到当前房间的根节点
            for (int room : entry.getValue()) {  // 遍历当前连接分量中的每个房间
                parent[find(parent, room)] = root;  // 将每个房间的根节点设置为当前房间的根节点
            }
        }
    }

    /** Generates a path between two points. */
    private void generatePath(int x1, int y1, int x2, int y2) {
        int x = x1;
        int y = y1;
        while (x != x2 || y != y2) {
            if (x < x2) x++;
            else if (x > x2) x--;
            else if (y < y2) y++;
            else if (y > y2) y--;
            tiles[x][y] = Tileset.FLOOR;
        }
    }

    /** Returns the tiles associated with this RandomWorld. */
    public TETile[][] getTiles() {
        return tiles;
    }

    public int hvalue(int Xcurr, int Ycurr, int Xtarget, int Ytarget){
        return Math.abs(Xcurr - Xtarget) + Math.abs(Ycurr - Ytarget);
    }

    public void Astar(int X1, int Y1, int X2, int Y2) {
        if (tiles[X1][Y1] == Tileset.WALL || tiles[X2][Y2] == Tileset.WALL || tiles[X1][Y1] == Tileset.NOTHING || tiles[X2][Y2] == Tileset.NOTHING) {
            System.out.println("Invalid input");
            return;
        }

        PriorityQueue<int[]> fringe = new PriorityQueue<>(new Comparator<int[]>() {
            @Override
            public int compare(int[] o1, int[] o2) {
                int f1 = o1[2] + o1[3];
                int f2 = o2[2] + o2[3];
                return Integer.compare(f1, f2);
            }
        });

        int[] start = {X1, Y1, 0, hvalue(X1, Y1, X2, Y2)};
        fringe.add(start);

        int[][] parent = new int[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                parent[i][j] = -1;
            }
        }
        parent[X1][Y1] = 0;

        while (!fringe.isEmpty()) {
            int[] curr = fringe.poll();
            int Xcurr = curr[0];
            int Ycurr = curr[1];

            if (Xcurr == X2 && Ycurr == Y2) {
                // 找到路径后开始绘制
                int X = X2;
                int Y = Y2;
                while (X != X1 || Y != Y1) {
                    int index = parent[X][Y];
                    int Xparent = index / height;
                    int Yparent = index % height;
                    tiles[X][Y] = Tileset.PATH;  // 绘制路径
                    X = Xparent;
                    Y = Yparent;
                }
                tiles[X2][Y2] = Tileset.COIN;  // 绘制路径
                break;  // 找到路径后退出循环
            }

            int[][] neighbors = {{Xcurr + 1, Ycurr}, {Xcurr - 1, Ycurr}, {Xcurr, Ycurr + 1}, {Xcurr, Ycurr - 1}};
            for (int[] neighbor : neighbors) {
                int Xneighbor = neighbor[0];
                int Yneighbor = neighbor[1];
                if (Xneighbor >= 0 && Xneighbor < width && Yneighbor >= 0 && Yneighbor < height) {
                    if (tiles[Xneighbor][Yneighbor] != Tileset.WALL && tiles[Xneighbor][Yneighbor] != Tileset.NOTHING) {
                        if (parent[Xneighbor][Yneighbor] == -1) {
                            parent[Xneighbor][Yneighbor] = Xcurr * height + Ycurr;
                            int[] next = {Xneighbor, Yneighbor, curr[2] + 1, hvalue(Xneighbor, Yneighbor, X2, Y2)};
                            fringe.add(next);
                        }
                    }
                }
            }
        }
    }


    public static void main(String[] args) {
        // Change these parameters as necessary

        int width = 80;
        int height = 40;
        long seed = System.currentTimeMillis();
        InputSource inputSource;

        inputSource = new KeyboardInputSource();

        int totalCharacters = 0;
        int Xcoin = 0;
        int Ycoin = 0;


        RandomWorld randomWorld = new RandomWorld(width, height, seed);
        TERenderer ter = new TERenderer();
        ter.initialize(width, height);
        ter.renderFrame(randomWorld.getTiles());
        for (int i =0; i < randomWorld.width; i++){
            for (int j = 0; j < randomWorld.height; j++){
                if (randomWorld.tiles[i][j] ==  Tileset.COIN){
                    Xcoin = i;
                    Ycoin = j;
                    break;
                }
            }
        }
        //get the location of the avatar
        int X1 = randomWorld.avatarX;
        int Y1 = randomWorld.avatarY;
        randomWorld.Astar(X1, Y1, Xcoin, Ycoin);

        while (inputSource.possibleNextInput()) {
            totalCharacters += 1;
            char c = inputSource.getNextKey();
            randomWorld.moveAvatar(c);
            randomWorld.eatCoin();
            ter.renderFrame(randomWorld.getTiles());
        }


    }

    /** Represents a room in the world. */
    private static class Room {
        int x, y, width, height;

        Room(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }
    }

    /** Represents an edge between two rooms. */
    private static class Edge {
        int room1, room2;
        double distance;

        Edge(int room1, int room2, double distance) {
            this.room1 = room1;
            this.room2 = room2;
            this.distance = distance;
        }
    }
}
