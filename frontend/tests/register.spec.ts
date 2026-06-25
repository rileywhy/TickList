import { test, expect } from "@playwright/test";
import { makeTestUser, registerUser } from "./helpers/users";

test("register a user", async ({ page }) => {
    const User = makeTestUser();
    await registerUser(page, User);

    //<p>Account created successfully!</p>
    await expect(page.getByText("Account created successfully!"));
});