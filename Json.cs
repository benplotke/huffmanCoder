using System;
using System.Collections.Generic;
using System.Text;

namespace AocTest
{
    class Json
    {
        public object content;

        public IEnumerable<T> GetLeaves<T>()
        {
            Stack<Json> nodes = new Stack<Json>();
            nodes.Push(this);
            while (nodes.Count > 0)
            {
                Json node = nodes.Pop();
                if (node.content is Dictionary<string, Json> dictionary)
                {
                    foreach (KeyValuePair<string, Json> keyValuePair in dictionary)
                    {
                        nodes.Push(keyValuePair.Value);
                    }
                }
                else if (node.content is List<Json> list)
                {
                    foreach (Json value in list)
                    {
                        nodes.Push(value);
                    }
                }
                else if (node.content is T t)
                {
                    yield return t;
                }
            }
        }

        private enum States
        {
            Start,
            ObjectStart,
            ObjectPreColon,
            ObjectMid,
            ObjectPostComma,
            ArrayStart,
            ArrayMid,
            ValueStart,
            ValueEnd,
            String,
            Number,
            True,
            False,
            Null
        }

        public static Json Deserialize(string json)
        {
            string workingString = null;

            States state = States.Start;
            Stack<States> stateStack = new Stack<States>();

            Stack<string> keyStack = new Stack<string>();

            Json root = new Json();
            Stack<Json> objectStack = new Stack<Json>();

            foreach (char c in json)
            {
                switch (state)
                {
                    case States.Start:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }

                        stateStack.Push(state);
                        if (c == '{')
                        {
                            root.content = new Dictionary<string, Json>();
                            state = States.ObjectStart;
                        }
                        else if (c == '[')
                        {
                            root.content = new List<Json>();
                            state = States.ArrayStart;
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.ObjectStart:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }
                        else if (c == '"')
                        {
                            stateStack.Push(state);
                            state = States.String;
                            workingString = "";
                        }
                        else if (c == '}')
                        {
                            state = stateStack.Pop();
                            if (state == States.ValueStart)
                            {
                                state = States.ValueEnd;
                            }
                            else
                            {
                                throw new Exception();
                            }
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.ObjectPreColon:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }
                        else if (c == ':')
                        {
                            objectStack.Push(root);
                            root = new Json();

                            stateStack.Push(state);
                            state = States.ValueStart;
                        }
                        break;

                    case States.ObjectMid:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }
                        else if (c == ',')
                        {
                            state = States.ObjectPostComma;
                        }
                        else if (c == '}')
                        {
                            state = stateStack.Pop();
                            if (state == States.ValueStart)
                            {
                                state = States.ValueEnd;
                            }
                            else
                            {
                                throw new Exception();
                            }
                        }
                        break;

                    case States.ObjectPostComma:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }
                        else if (c == '"')
                        {
                            stateStack.Push(state);
                            state = States.String;
                            workingString = "";
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.ArrayStart:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }
                        else if (c == ']')
                        {
                            state = stateStack.Pop();
                            if (state == States.ValueStart)
                            {
                                state = States.ValueEnd;
                            }
                            else
                            {
                                throw new Exception();
                            }
                        }
                        else
                        {
                            stateStack.Push(state);
                            state = States.ValueStart;

                            objectStack.Push(root);
                            root = new Json();

                            goto case States.ValueStart;
                        }
                        break;

                    case States.ArrayMid:
                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }
                        else if (c == ',')
                        {
                            objectStack.Push(root);
                            root = new Json();

                            stateStack.Push(state);
                            state = States.ValueStart;
                        }
                        else if (c == ']')
                        {
                            state = stateStack.Pop();
                            if (state == States.ValueStart)
                            {
                                state = States.ValueEnd;
                            }
                            else if (state == States.Start)
                            {
                                continue;
                            }
                            else
                            {
                                throw new Exception();
                            }
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.ValueStart:

                        if (char.IsWhiteSpace(c))
                        {
                            continue;
                        }

                        stateStack.Push(state);

                        if (c == '"')
                        {
                            workingString = "";
                            state = States.String;
                        }
                        else if (c == '{')
                        {
                            root.content = new Dictionary<string, Json>();
                            state = States.ObjectStart;
                        }
                        else if (c == '[')
                        {
                            root.content = new List<Json>();
                            state = States.ArrayStart;
                        }
                        else if ("-0123456789".Contains(c))
                        {
                            workingString = c.ToString();
                            state = States.Number;
                        }
                        else if (c == 't')
                        {
                            workingString = "t";
                            state = States.True;
                        }
                        else if (c == 'f')
                        {
                            workingString = "f";
                            state = States.False;
                        }
                        else if (c == 'n')
                        {
                            workingString = "n";
                            state = States.Null;
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.ValueEnd:

                        Json parent = objectStack.Pop();
                        state = stateStack.Pop();

                        if (state == States.ArrayStart || state == States.ArrayMid)
                        {
                            ((List<Json>)parent.content).Add(root);
                            root = parent;

                            state = States.ArrayMid;
                            goto case States.ArrayMid;
                        }

                        else if (state == States.ObjectPreColon)
                        {
                            string keyString = keyStack.Pop();

                            ((Dictionary<string, Json>)parent.content).Add(keyString, root);
                            root = parent;

                            state = States.ObjectMid;
                            goto case States.ObjectMid;
                        }
                        break;

                    case States.String:

                        if (c == '"')
                        {
                            state = stateStack.Pop();
                            if (state == States.ObjectStart || state == States.ObjectPostComma)
                            {
                                keyStack.Push(workingString);
                                state = States.ObjectPreColon;
                            }
                            else if (state == States.ValueStart)
                            {
                                root.content = workingString;
                                state = States.ValueEnd;
                            }
                            else
                            {
                                throw new Exception();
                            }
                        }
                        else
                        {
                            workingString += c;
                        }
                        break;

                    case States.Number:

                        if ("+-.0123456789eE".Contains(c))
                        {
                            workingString += c;
                        }
                        else
                        {
                            root.content = float.Parse(workingString);
                            stateStack.Pop();
                            state = States.ValueEnd;
                            goto case States.ValueEnd;
                        }
                        break;

                    case States.True:

                        if (c == 'r' && workingString.Equals("t"))
                        {
                            workingString += "r";
                        }
                        else if (c == 'u' && workingString.Equals("tr"))
                        {
                            workingString += "u";
                        }
                        else if (c == 'e' && workingString.Equals("tru"))
                        {
                            root.content = true;
                            state = stateStack.Pop();
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.False:

                        if (c == 'a' && workingString.Equals("f"))
                        {
                            workingString += "a";
                        }
                        else if (c == 'l' && workingString.Equals("fa"))
                        {
                            workingString += "l";
                        }
                        else if (c == 's' && workingString.Equals("fal"))
                        {
                            workingString += "s";
                        }
                        else if (c == 'e' && workingString.Equals("fals"))
                        {
                            root.content = false;
                            state = stateStack.Pop();
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;

                    case States.Null:

                        if (c == 'u' && workingString.Equals("n"))
                        {
                            workingString += "u";
                        }
                        else if (c == 'l' && workingString.Equals("nu"))
                        {
                            workingString += "l";
                        }
                        else if (c == 'l' && workingString.Equals("nul"))
                        {
                            root.content = null;
                            state = stateStack.Pop();
                        }
                        else
                        {
                            throw new Exception();
                        }
                        break;
                }
            }
            return root;
        }
    }
}
