[
   Colorer              -- V is=2 [H  [KW["colorer"] _1] V [_2]],
   Colorer.2:iter-star  -- _1,
   ColorDef             -- H  [_1 KW["="] _2],
   ColorRuleAll         -- H [ KW["environment"] _1 KW[":"] _2 ],
   ColorRule            -- H [ _1 KW[":"] _2 ],
   ColorRuleAllNamed    -- H [ KW["environment"] _1 KW[":"] _2 KW["="] _3 ],
   ColorRuleNamed       -- H [ _1 KW[":"]                   _2 KW["="] _3 ],
   Attribute            -- _1 _2 _3,
   Attribute            -- _1 _2 _3,
   AttributeRef         -- _1,
   FoldAll              -- KW["all"] _1,
   Values                 -- _1,
   Values.1:iter-star-sep -- _1 KW[","],
   Language             -- V is=2 [H  [KW["language"] _1] V [_2]],
   Language.2:iter-star -- _1,
   Name                 -- H [ KW["name"] KW[":"] _1 ],
   Id                   -- H [ KW["id"] KW[":"] _1 ],
   Extensions           -- H [ KW["extensions"] KW[":"] _1 ],
   Description          -- H [ KW["description"] KW[":"] _1 ],
   Table                -- H [ KW["table"] KW[":"] _1 ],
   StartSymbol          -- H [ KW["start"] KW["symbol"] KW[":"] _1 ],
   URL                  -- H [ KW["url"] KW[":"] _1 ],
   Extends              -- H [ KW["extends"] KW[":"] _1 ],
   Aliases              -- H [ KW["aliases"] KW[":"] _1 ],
   CommentLine          -- H  [KW["//"] _1],
   EmptyLine            -- V  [KW[""]],
   Token                -- _1,
   TK_IDENTIFIER        -- KW["identifier"],
   TK_NUMBER            -- KW["number"],
   TK_LAYOUT            -- KW["layout"],
   TK_STRING            -- KW["string"],
   TK_KEYWORD           -- KW["keyword"],
   TK_OPERATOR          -- KW["operator"],
   TK_VAR               -- KW["var"],
   TK_JUNK              -- KW["junk"],
   TK_UNKNOWN           -- KW["unknown"],
   NORMAL               -- ,
   BOLD                 -- KW["bold"],
   ITALIC               -- KW["italic"],
   ColorDefault         -- KW["_"],
   ColorRGB             -- H  [_1 _2 _3],
   Outliner             -- V is=2 [H  [KW["outliner"] _1] V [_2]],
   Outliner.2:iter-star -- _1,
   Folding              -- V is=2 [H  [KW["folding"] _1] V [_2]],
   Folding.2:iter-star  -- _1,
   Sort                 -- _1,
   ListSort             -- _1 KW["*"],
   ConstructorOnly      -- H hs=0 [KW["_"] KW["."] _1],
   Constructor          -- _1,
   SortAndConstructor   -- H hs=0 [_1 KW["."] _2],
   Module               -- V vs = 1 [H  [KW["module"] _1] _2 _3 ],
   Module.3:iter-star   -- _1,
   Imports              -- V is=2 [KW["imports"] _1],
   Imports.1:iter       -- _1,
   Import               -- _1,
   ImportRenamed        -- H hs=0 [_1 KW["["] _2 KW["]"]],
   NoImports            -- 
]

