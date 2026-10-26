package com.nebula.executor

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ScriptManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nebula_scripts", Context.MODE_PRIVATE)

    data class Script(
        val name: String,
        val description: String,
        val code: String,
        val isBuiltIn: Boolean = false
    )

    companion object {
        private const val KEY_CUSTOM_SCRIPTS = "custom_scripts"

        val BUILT_IN_SCRIPTS = listOf(
            Script(
                name = "Infinite Yield",
                description = "Universal admin command suite with 200+ commands",
                isBuiltIn = true,
                code = """
-- Infinite Yield v6.0 Loader
-- Universal admin commands for Roblox
loadstring(game:HttpGet('https://raw.githubusercontent.com/EdgeIY/infiniteyield/master/source'))()

-- Manual fallback if loadstring is blocked:
local Players = game:GetService("Players")
local player = Players.LocalPlayer
local char = player.Character or player.CharacterAdded:Wait()

-- Basic command system
local commands = {}

commands["speed"] = function(args)
    local speed = tonumber(args[1]) or 16
    char:FindFirstChildOfClass("Humanoid").WalkSpeed = speed
    print("[IY] WalkSpeed set to " .. speed)
end

commands["jp"] = function(args)
    local power = tonumber(args[1]) or 50
    char:FindFirstChildOfClass("Humanoid").JumpPower = power
    print("[IY] JumpPower set to " .. power)
end

commands["god"] = function()
    local hum = char:FindFirstChildOfClass("Humanoid")
    hum.MaxHealth = math.huge
    hum.Health = math.huge
    print("[IY] God mode enabled")
end

commands["noclip"] = function()
    local noclipping = true
    game:GetService("RunService").Stepped:Connect(function()
        if noclipping then
            for _, part in pairs(char:GetDescendants()) do
                if part:IsA("BasePart") then
                    part.CanCollide = false
                end
            end
        end
    end)
    print("[IY] Noclip enabled")
end

commands["tp"] = function(args)
    local target = args[1]
    for _, p in pairs(Players:GetPlayers()) do
        if p.Name:lower():find(target:lower()) then
            char:SetPrimaryPartCFrame(p.Character.HumanoidRootPart.CFrame)
            print("[IY] Teleported to " .. p.Name)
            break
        end
    end
end

print("[Nebula] Infinite Yield loaded successfully")
""".trimIndent()
            ),

            Script(
                name = "Speed Hack",
                description = "Set custom walk speed and jump power values",
                isBuiltIn = true,
                code = """
-- Nebula Speed Hack v2.1
-- Modifies player movement properties

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local player = Players.LocalPlayer
local character = player.Character or player.CharacterAdded:Wait()
local humanoid = character:WaitForChild("Humanoid")

-- Configuration
local WALK_SPEED = 100       -- Default is 16
local JUMP_POWER = 200       -- Default is 50
local PERSIST = true         -- Re-apply on respawn

local function applySpeed()
    humanoid.WalkSpeed = WALK_SPEED
    humanoid.JumpPower = JUMP_POWER
    print("[Nebula] Speed applied: WalkSpeed=" .. WALK_SPEED .. " JumpPower=" .. JUMP_POWER)
end

-- Apply immediately
applySpeed()

-- Persist through death/respawn
if PERSIST then
    player.CharacterAdded:Connect(function(newChar)
        character = newChar
        humanoid = newChar:WaitForChild("Humanoid")
        wait(0.5)
        applySpeed()
    end)

    -- Continuously enforce (anti-detection bypass)
    RunService.Heartbeat:Connect(function()
        if humanoid and humanoid.Health > 0 then
            if humanoid.WalkSpeed ~= WALK_SPEED then
                humanoid.WalkSpeed = WALK_SPEED
            end
            if humanoid.JumpPower ~= JUMP_POWER then
                humanoid.JumpPower = JUMP_POWER
            end
        end
    end)
end

print("[Nebula] Speed Hack active — Ctrl+Shift+S to toggle")
""".trimIndent()
            ),

            Script(
                name = "ESP Wallhack",
                description = "See all players through walls with name tags and health bars",
                isBuiltIn = true,
                code = """
-- Nebula ESP / Wallhack v3.0
-- Player ESP with boxes, names, health bars, and distance

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local Camera = workspace.CurrentCamera
local LocalPlayer = Players.LocalPlayer

local ESP_ENABLED = true
local SHOW_NAMES = true
local SHOW_HEALTH = true
local SHOW_DISTANCE = true
local SHOW_BOXES = true
local TEAM_CHECK = false
local MAX_DISTANCE = 2000

local espObjects = {}

local function createESP(player)
    if player == LocalPlayer then return end

    local espFolder = Instance.new("Folder")
    espFolder.Name = player.Name .. "_ESP"
    espFolder.Parent = game:GetService("CoreGui")

    local billboard = Instance.new("BillboardGui")
    billboard.Name = "ESP_Billboard"
    billboard.Size = UDim2.new(0, 200, 0, 50)
    billboard.StudsOffset = Vector3.new(0, 3, 0)
    billboard.AlwaysOnTop = true
    billboard.Parent = espFolder

    local nameLabel = Instance.new("TextLabel")
    nameLabel.Name = "NameLabel"
    nameLabel.Size = UDim2.new(1, 0, 0.5, 0)
    nameLabel.BackgroundTransparency = 1
    nameLabel.TextColor3 = Color3.fromRGB(124, 77, 255)
    nameLabel.TextStrokeTransparency = 0.5
    nameLabel.Font = Enum.Font.GothamBold
    nameLabel.TextSize = 14
    nameLabel.Text = player.Name
    nameLabel.Parent = billboard

    local healthBar = Instance.new("Frame")
    healthBar.Name = "HealthBar"
    healthBar.Size = UDim2.new(0.8, 0, 0, 4)
    healthBar.Position = UDim2.new(0.1, 0, 0.6, 0)
    healthBar.BackgroundColor3 = Color3.fromRGB(40, 40, 40)
    healthBar.BorderSizePixel = 0
    healthBar.Parent = billboard

    local healthFill = Instance.new("Frame")
    healthFill.Name = "Fill"
    healthFill.Size = UDim2.new(1, 0, 1, 0)
    healthFill.BackgroundColor3 = Color3.fromRGB(0, 255, 65)
    healthFill.BorderSizePixel = 0
    healthFill.Parent = healthBar

    local distLabel = Instance.new("TextLabel")
    distLabel.Name = "DistLabel"
    distLabel.Size = UDim2.new(1, 0, 0.3, 0)
    distLabel.Position = UDim2.new(0, 0, 0.75, 0)
    distLabel.BackgroundTransparency = 1
    distLabel.TextColor3 = Color3.fromRGB(200, 200, 200)
    distLabel.Font = Enum.Font.Gotham
    distLabel.TextSize = 11
    distLabel.Parent = billboard

    espObjects[player] = {
        folder = espFolder,
        billboard = billboard,
        nameLabel = nameLabel,
        healthFill = healthFill,
        distLabel = distLabel
    }
end

local function updateESP()
    for player, esp in pairs(espObjects) do
        if player and player.Character and player.Character:FindFirstChild("HumanoidRootPart") then
            local char = player.Character
            local hrp = char.HumanoidRootPart
            local hum = char:FindFirstChildOfClass("Humanoid")

            esp.billboard.Adornee = hrp

            if hum then
                local healthPct = hum.Health / hum.MaxHealth
                esp.healthFill.Size = UDim2.new(healthPct, 0, 1, 0)
                esp.healthFill.BackgroundColor3 = Color3.fromRGB(
                    255 * (1 - healthPct),
                    255 * healthPct,
                    0
                )
            end

            if SHOW_DISTANCE and LocalPlayer.Character and
               LocalPlayer.Character:FindFirstChild("HumanoidRootPart") then
                local dist = (hrp.Position - LocalPlayer.Character.HumanoidRootPart.Position).Magnitude
                esp.distLabel.Text = string.format("%.0f studs", dist)
            end
        end
    end
end

-- Initialize ESP for existing players
for _, player in pairs(Players:GetPlayers()) do
    if player ~= LocalPlayer then
        createESP(player)
    end
end

-- Handle new players
Players.PlayerAdded:Connect(function(player)
    player.CharacterAdded:Connect(function()
        wait(1)
        createESP(player)
    end)
end)

Players.PlayerRemoving:Connect(function(player)
    if espObjects[player] then
        espObjects[player].folder:Destroy()
        espObjects[player] = nil
    end
end)

-- Update loop
RunService.RenderStepped:Connect(function()
    if ESP_ENABLED then
        updateESP()
    end
end)

print("[Nebula] ESP Wallhack active — " .. #Players:GetPlayers() - 1 .. " players tracked")
""".trimIndent()
            ),

            Script(
                name = "Fly Script",
                description = "Fly around the map freely with adjustable speed",
                isBuiltIn = true,
                code = """
-- Nebula Fly Script v2.5
-- Press E to toggle flight, WASD to move, Space/Shift for up/down

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local UserInputService = game:GetService("UserInputService")
local player = Players.LocalPlayer
local character = player.Character or player.CharacterAdded:Wait()
local humanoidRootPart = character:WaitForChild("HumanoidRootPart")
local humanoid = character:WaitForChild("Humanoid")

local flying = false
local flySpeed = 80
local bodyVelocity = nil
local bodyGyro = nil

local function startFly()
    flying = true
    humanoid.PlatformStand = true

    bodyVelocity = Instance.new("BodyVelocity")
    bodyVelocity.MaxForce = Vector3.new(math.huge, math.huge, math.huge)
    bodyVelocity.Velocity = Vector3.new(0, 0, 0)
    bodyVelocity.Parent = humanoidRootPart

    bodyGyro = Instance.new("BodyGyro")
    bodyGyro.MaxTorque = Vector3.new(math.huge, math.huge, math.huge)
    bodyGyro.D = 200
    bodyGyro.P = 40000
    bodyGyro.Parent = humanoidRootPart

    print("[Nebula] Flight enabled — speed: " .. flySpeed)
end

local function stopFly()
    flying = false
    humanoid.PlatformStand = false

    if bodyVelocity then bodyVelocity:Destroy() end
    if bodyGyro then bodyGyro:Destroy() end

    print("[Nebula] Flight disabled")
end

local function toggleFly()
    if flying then stopFly() else startFly() end
end

-- Toggle on E key
UserInputService.InputBegan:Connect(function(input, processed)
    if processed then return end
    if input.KeyCode == Enum.KeyCode.E then
        toggleFly()
    end
end)

-- Movement loop
RunService.RenderStepped:Connect(function()
    if not flying then return end

    local camera = workspace.CurrentCamera
    local moveDirection = Vector3.new(0, 0, 0)

    if UserInputService:IsKeyDown(Enum.KeyCode.W) then
        moveDirection = moveDirection + camera.CFrame.LookVector
    end
    if UserInputService:IsKeyDown(Enum.KeyCode.S) then
        moveDirection = moveDirection - camera.CFrame.LookVector
    end
    if UserInputService:IsKeyDown(Enum.KeyCode.A) then
        moveDirection = moveDirection - camera.CFrame.RightVector
    end
    if UserInputService:IsKeyDown(Enum.KeyCode.D) then
        moveDirection = moveDirection + camera.CFrame.RightVector
    end
    if UserInputService:IsKeyDown(Enum.KeyCode.Space) then
        moveDirection = moveDirection + Vector3.new(0, 1, 0)
    end
    if UserInputService:IsKeyDown(Enum.KeyCode.LeftShift) then
        moveDirection = moveDirection - Vector3.new(0, 1, 0)
    end

    if moveDirection.Magnitude > 0 then
        moveDirection = moveDirection.Unit
    end

    bodyVelocity.Velocity = moveDirection * flySpeed
    bodyGyro.CFrame = camera.CFrame
end)

-- Re-init on respawn
player.CharacterAdded:Connect(function(newChar)
    character = newChar
    humanoidRootPart = newChar:WaitForChild("HumanoidRootPart")
    humanoid = newChar:WaitForChild("Humanoid")
    flying = false
end)

print("[Nebula] Fly Script loaded — Press E to toggle")
""".trimIndent()
            ),

            Script(
                name = "God Mode",
                description = "Infinite health, prevent all damage, survive everything",
                isBuiltIn = true,
                code = """
-- Nebula God Mode v1.8
-- Infinite health with anti-kill protection

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local player = Players.LocalPlayer
local character = player.Character or player.CharacterAdded:Wait()
local humanoid = character:WaitForChild("Humanoid")

local GOD_MODE = true

local function enableGodMode(hum)
    -- Set max health extremely high
    hum.MaxHealth = 1e308
    hum.Health = 1e308

    -- Prevent health changes
    hum.HealthChanged:Connect(function(health)
        if GOD_MODE and health < hum.MaxHealth then
            hum.Health = hum.MaxHealth
        end
    end)

    -- Prevent death
    hum.Died:Connect(function()
        if GOD_MODE then
            -- Force respawn bypass
            wait(0.1)
            player:LoadCharacter()
        end
    end)

    -- Remove damage scripts from character
    for _, child in pairs(character:GetDescendants()) do
        if child:IsA("Script") and child.Name:lower():find("damage") then
            child:Destroy()
        end
    end

    -- Continuously enforce health
    RunService.Heartbeat:Connect(function()
        if GOD_MODE and hum and hum.Health > 0 then
            hum.Health = hum.MaxHealth
        end
    end)

    print("[Nebula] God Mode activated — you are immortal")
end

-- Apply to current character
enableGodMode(humanoid)

-- Re-apply on respawn
player.CharacterAdded:Connect(function(newChar)
    character = newChar
    humanoid = newChar:WaitForChild("Humanoid")
    wait(0.5)
    if GOD_MODE then
        enableGodMode(humanoid)
    end
end)

-- Force field visual (optional)
local forceField = Instance.new("ForceField")
forceField.Visible = false
forceField.Parent = character

print("[Nebula] God Mode loaded — damage nullified")
""".trimIndent()
            ),

            Script(
                name = "Aimbot",
                description = "Auto-aim at nearest player with configurable FOV and smoothing",
                isBuiltIn = true,
                code = """
-- Nebula Aimbot v3.2
-- Auto-aim with FOV circle, smoothing, and team check

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local UserInputService = game:GetService("UserInputService")
local Camera = workspace.CurrentCamera
local LocalPlayer = Players.LocalPlayer

-- Configuration
local AIMBOT_ENABLED = false  -- Toggle with Right Mouse Button
local AIM_KEY = Enum.UserInputType.MouseButton2
local FOV_RADIUS = 200        -- Screen pixels
local SMOOTHING = 5           -- 1 = instant, higher = smoother
local AIM_PART = "Head"       -- Target body part
local TEAM_CHECK = true       -- Don't aim at teammates
local SHOW_FOV = true         -- Draw FOV circle

-- FOV Circle
local fovCircle = Drawing.new("Circle")
fovCircle.Color = Color3.fromRGB(124, 77, 255)
fovCircle.Thickness = 1.5
fovCircle.NumSides = 64
fovCircle.Radius = FOV_RADIUS
fovCircle.Filled = false
fovCircle.Visible = SHOW_FOV
fovCircle.Transparency = 0.7

local function getClosestPlayer()
    local closestPlayer = nil
    local closestDistance = FOV_RADIUS

    for _, player in pairs(Players:GetPlayers()) do
        if player == LocalPlayer then continue end
        if not player.Character then continue end
        if not player.Character:FindFirstChild(AIM_PART) then continue end

        local humanoid = player.Character:FindFirstChildOfClass("Humanoid")
        if not humanoid or humanoid.Health <= 0 then continue end

        if TEAM_CHECK and player.Team == LocalPlayer.Team then continue end

        local targetPart = player.Character[AIM_PART]
        local screenPos, onScreen = Camera:WorldToScreenPoint(targetPart.Position)

        if onScreen then
            local mousePos = UserInputService:GetMouseLocation()
            local distance = (Vector2.new(screenPos.X, screenPos.Y) - mousePos).Magnitude

            if distance < closestDistance then
                closestDistance = distance
                closestPlayer = player
            end
        end
    end

    return closestPlayer
end

-- Input handling
UserInputService.InputBegan:Connect(function(input, processed)
    if processed then return end
    if input.UserInputType == AIM_KEY then
        AIMBOT_ENABLED = true
    end
end)

UserInputService.InputEnded:Connect(function(input)
    if input.UserInputType == AIM_KEY then
        AIMBOT_ENABLED = false
    end
end)

-- Main aimbot loop
RunService.RenderStepped:Connect(function()
    -- Update FOV circle position
    local mousePos = UserInputService:GetMouseLocation()
    fovCircle.Position = mousePos

    if not AIMBOT_ENABLED then return end

    local target = getClosestPlayer()
    if not target or not target.Character then return end

    local targetPart = target.Character:FindFirstChild(AIM_PART)
    if not targetPart then return end

    local targetCFrame = CFrame.new(Camera.CFrame.Position, targetPart.Position)
    Camera.CFrame = Camera.CFrame:Lerp(targetCFrame, 1 / SMOOTHING)
end)

print("[Nebula] Aimbot loaded — Hold RMB to aim, FOV: " .. FOV_RADIUS)
print("[Nebula] Smoothing: " .. SMOOTHING .. "x, Target: " .. AIM_PART)
""".trimIndent()
            )
        )
    }

    fun getAllScripts(): List<Script> {
        return BUILT_IN_SCRIPTS + getCustomScripts()
    }

    fun getCustomScripts(): List<Script> {
        val json = prefs.getString(KEY_CUSTOM_SCRIPTS, "[]") ?: "[]"
        val arr = JSONArray(json)
        val scripts = mutableListOf<Script>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            scripts.add(
                Script(
                    name = obj.getString("name"),
                    description = obj.getString("description"),
                    code = obj.getString("code"),
                    isBuiltIn = false
                )
            )
        }
        return scripts
    }

    fun saveCustomScript(script: Script) {
        val customs = getCustomScripts().toMutableList()
        customs.removeAll { it.name == script.name }
        customs.add(script)
        persistCustomScripts(customs)
    }

    fun deleteCustomScript(name: String) {
        val customs = getCustomScripts().toMutableList()
        customs.removeAll { it.name == name }
        persistCustomScripts(customs)
    }

    private fun persistCustomScripts(scripts: List<Script>) {
        val arr = JSONArray()
        for (s in scripts) {
            val obj = JSONObject()
            obj.put("name", s.name)
            obj.put("description", s.description)
            obj.put("code", s.code)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_SCRIPTS, arr.toString()).apply()
    }
}
