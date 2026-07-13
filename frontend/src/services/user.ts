import { api } from "@/lib/api";

export const UserService = {
  async updateProfile(name: string): Promise<any> {
    // Placeholder for user profile settings update
    console.log("Profile update triggered for:", name);
    return { success: true };
  },
};
