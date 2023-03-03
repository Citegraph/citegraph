import { Form, useLoaderData } from "react-router-dom";
import { getAuthor } from "../authors";

export async function loader({ params }) {
  const author = await getAuthor(params.authorId);
  console.log("author is", author);
  return { author };
}

export default function Author() {
  const { author } = useLoaderData();

  return (
    <div id="author">
      <div>
        {/* <img
          key={author.avatar}
          src={author.avatar || null}
        /> */}
      </div>

      <div>
        <h1>
          {author.name}
        </h1>
      </div>
    </div>
  );
}