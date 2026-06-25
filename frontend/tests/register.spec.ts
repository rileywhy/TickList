import { test, expect } from "@playwright/test";
import { registerUser } from "./helpers/users";

test("register a user", async ({ page }) => {
    await registerUser(page);
    //<p>Account created successfully!</p>
    await expect(page.getByText("Account created successfully!"));
});